package ru.sigmacall.sigma.sip;

import android.content.Context;
import android.gov.nist.javax.sdp.SessionDescriptionImpl;
import android.gov.nist.javax.sdp.parser.SDPAnnounceParser;
import android.gov.nist.javax.sip.SipStackExt;
import android.gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import android.javax.sdp.MediaDescription;
import android.javax.sdp.SdpException;
import android.javax.sip.ClientTransaction;
import android.javax.sip.Dialog;
import android.javax.sip.DialogTerminatedEvent;
import android.javax.sip.IOExceptionEvent;
import android.javax.sip.InvalidArgumentException;
import android.javax.sip.ListeningPoint;
import android.javax.sip.ObjectInUseException;
import android.javax.sip.PeerUnavailableException;
import android.javax.sip.RequestEvent;
import android.javax.sip.ResponseEvent;
import android.javax.sip.ServerTransaction;
import android.javax.sip.SipException;
import android.javax.sip.SipFactory;
import android.javax.sip.SipListener;
import android.javax.sip.SipProvider;
import android.javax.sip.TimeoutEvent;
import android.javax.sip.TransactionTerminatedEvent;
import android.javax.sip.TransactionUnavailableException;
import android.javax.sip.TransportNotSupportedException;
import android.javax.sip.address.Address;
import android.javax.sip.address.AddressFactory;
import android.javax.sip.address.SipURI;
import android.javax.sip.address.URI;
import android.javax.sip.header.CSeqHeader;
import android.javax.sip.header.CallIdHeader;
import android.javax.sip.header.ContactHeader;
import android.javax.sip.header.ContentTypeHeader;
import android.javax.sip.header.FromHeader;
import android.javax.sip.header.HeaderFactory;
import android.javax.sip.header.MaxForwardsHeader;
import android.javax.sip.header.ToHeader;
import android.javax.sip.header.ViaHeader;
import android.javax.sip.message.MessageFactory;
import android.javax.sip.message.Request;
import android.javax.sip.message.Response;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.TooManyListenersException;

import ru.sigmacall.sigma.AppConf;
import ru.sigmacall.sigma.SigmaApp;
import ru.sigmacall.sigma.sip.listeners.OnSipRequestListener;
import ru.sigmacall.sigma.tools.LogHelper;

public class SipStack implements SipListener{
    public static final String TAG = "SipStack: ";

    private static boolean WRITE_LOG_TO_FILE = true;

    private SipProfile sipProfile;
    private android.javax.sip.SipStack sipStack;

    private MessageFactory messageFactory;
    private HeaderFactory headerFactory;
    private AddressFactory addressFactory;
    private ListeningPoint listeningPoint;
    private SipProvider sipProvider;
    private SoundManager soundManager;

    private ClientTransaction inviteTid;

    private OnSipRequestListener mOnSipRequestListener;

    private SipState state = SipState.IDLE;

    public void setmOnSipRequestListener(OnSipRequestListener mOnSipRequestListener) {
        this.mOnSipRequestListener = mOnSipRequestListener;
    }

    private Dialog dialog;

    private static Context context = null;

    public SipStack(Context context, SipProfile sipProfile) {
        this.context = context;
        this.sipProfile = sipProfile;
        this.soundManager = new SoundManager(context, sipProfile.getLocalIp(), this);
        init();
    }

    private void init() {
        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("android.gov.nist");

        Properties props = new Properties();
        props.setProperty("android.javax.sip.STACK_NAME", "PanoSipStack");
        props.setProperty("android.javax.sip.OUTBOUND_PROXY",
                sipProfile.getSrvHostPort() + "/" + sipProfile.getProtocol());

        try {
            sipStack = sipFactory.createSipStack(props);
            log("Create SipStack: " + sipStack);
        } catch (PeerUnavailableException e) {
            e.printStackTrace();
            log(e.getMessage());
        }

        try {
            messageFactory = sipFactory.createMessageFactory();
            headerFactory = sipFactory.createHeaderFactory();
            addressFactory = sipFactory.createAddressFactory();

            listeningPoint = sipStack.createListeningPoint(
                    sipProfile.getLocalIp(),
                    sipProfile.getLocalPort(),
                    sipProfile.getProtocol()
            );
            sipProvider = sipStack.createSipProvider(listeningPoint);
            sipProvider.addSipListener(this);

            log("Stack initialized successfully!");
        } catch (PeerUnavailableException |
                InvalidArgumentException |
                TransportNotSupportedException |
                ObjectInUseException |
                TooManyListenersException e) {
            e.printStackTrace();
            log(e.getMessage());
            setState(SipState.ERROR_INIT);
        }
    }

    public void clear() {
        try {
            speakerPhone(false);
            micMute(false);
            sipStack.deleteListeningPoint(listeningPoint);
            soundManager.releaseAudioResourcesFull();
            if (sipProvider != null) {
                sipProvider.removeSipListener(this);
                sipStack.deleteSipProvider(sipProvider);
            }
        } catch (ObjectInUseException e) {
            e.printStackTrace();
        }
    }

    public void drop_call() {
        if (state == SipState.IDLE) return;
        SigmaApp.log("Call interrupted by user. Status: " + state.toString(), TAG);
        soundManager.releaseAudioResources();
        if (state == SipState.CALLING) sendCancel();
        else sendBye();

        setState(SipState.IDLE);
    }

    private void sendBye() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SigmaApp.log("Sending BYE request", TAG);
                try {
                    Request byeRequest = dialog.createRequest(Request.BYE);
                    ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
                    log(ct.toString());
                    dialog.sendRequest(ct);
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void sendCancel() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                SigmaApp.log("Sending CANCEL request", TAG);
                try {
                    Request cancelRequest = inviteTid.createCancel();
                    ClientTransaction cancelTid = sipProvider.getNewClientTransaction(cancelRequest);
                    log(cancelRequest.toString());
                    cancelTid.sendRequest();
                } catch (SipException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void drop_call_busy() {
        soundManager.releaseAudioResources();
        setState(SipState.IDLE);
        mOnSipRequestListener.OnBusy();
    }

    //!
    public void sessionProgress() {
        soundManager.releaseAudioResources();
        if (state == SipState.SESSION_PROGRESS) {
            sendCancel();
        }
        setState(SipState.IDLE);
    }

    public void register() {
        log("Register attempt");

        CallIdHeader callIdHeader = sipProvider.getNewCallId();
        try {
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.REGISTER);

            Address userAddress = addressFactory.createAddress(
                    "sip:" + sipProfile.getUsr() + "@" + sipProfile.getLocalHostPort()
                            + ";transport=" + sipProfile.getProtocol()
                            + ";registering_acc=" + sipProfile.getSrv());

            ContactHeader userContactHeader = headerFactory.createContactHeader(userAddress);
            Address fromAddress = addressFactory.createAddress(
                    "sip:" + sipProfile.getUsr() + "@" + sipProfile.getSrv()
            );
            FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, "123456");


            ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader;
            viaHeader = headerFactory.createViaHeader(
                    sipProfile.getLocalIp(),
                    sipProfile.getLocalPort(),
                    sipProfile.getProtocol(),
                    null
            );
            viaHeaders.add(viaHeader);

            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

            ToHeader toHeader = headerFactory.createToHeader(fromAddress, null);

            URI requestUri = addressFactory.createAddress(
                    "sip:" + sipProfile.getSrvHostPort()
            ).getURI();
            Request request = messageFactory.createRequest(
                    requestUri,
                    Request.REGISTER,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader
            );

            request.addHeader(userContactHeader);
            request.addHeader(headerFactory.createExpiresHeader(300));

            log("Request constructed");
            System.out.println(request.toString());

            // Send request
            try {
                log(request.toString());

                final ClientTransaction ta = sipProvider.getNewClientTransaction(request);
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            ta.sendRequest();
                        } catch (SipException e) {
                            e.printStackTrace();
                            log("Error send REGISTER request: " + e.getMessage());
                        }
                    }
                };
                thread.start();
            } catch (TransactionUnavailableException e) {
                e.printStackTrace();
            }

        } catch (ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            log("Register Error: " + e.getMessage());
        }
    }

    public void invite(String toUsr) {
        if (state == SipState.IN_CONVERSATION) return;
        setState(SipState.CALLING);
        try {
            Address userAddress = addressFactory.createAddress(
                    "sip:" + sipProfile.getUsr() + "@" + sipProfile.getLocalHostPort()
                            + ";transport=" + sipProfile.getProtocol()
                            + ";registering_acc=" + sipProfile.getSrv());

            ContactHeader userContactHeader = headerFactory.createContactHeader(userAddress);
            Address fromAddress = addressFactory.createAddress(
                    "sip:" + sipProfile.getUsr() + "@" + sipProfile.getSrv()
            );
            FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, "123456");


            ArrayList<ViaHeader> viaHeaders = new ArrayList<>();
            ViaHeader viaHeader;
            viaHeader = headerFactory.createViaHeader(
                    sipProfile.getLocalIp(),
                    sipProfile.getLocalPort(),
                    sipProfile.getProtocol(),
                    null
            );
            viaHeaders.add(viaHeader);

            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(70);

            // To Header
            log("Call To user: " + toUsr);
            SipURI toSipUri = addressFactory.createSipURI(toUsr, sipProfile.getSrv());
            Address toAddress = addressFactory.createAddress(toSipUri);
            ToHeader toHeader = headerFactory.createToHeader(toAddress, null);

            // Request URI
            SipURI requestUri = addressFactory.createSipURI(toUsr, sipProfile.getSrvHostPort());

            // Content-type header
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(
                    "application", "sdp"
            );

            // CalId header
            CallIdHeader callIdHeader = sipProvider.getNewCallId();

            // Create new Cseq header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

            Request request = messageFactory.createRequest(
                    requestUri,
                    Request.INVITE,
                    callIdHeader,
                    cSeqHeader,
                    fromHeader,
                    toHeader,
                    viaHeaders,
                    maxForwardsHeader
            );

            request.addHeader(userContactHeader);

            int localRtpPort = soundManager.setupLocalStream();

            String sdpData= "v=0\r\n"
                    + "o=4855 13760799956958020 13760799956958020"
                    + " IN IP4 " + sipProfile.getLocalIp() +"\r\n" + "s=mysession session\r\n"
                    + "p=+46 8 52018010\r\n" + "c=IN IP4 " + sipProfile.getLocalIp()+"\r\n"
                    + "t=0 0\r\n" + "m=audio "+ localRtpPort +" RTP/AVP 0 4 18\r\n"
                    + "a=rtpmap:0 PCMU/8000\r\n" + "a=rtpmap:4 G723/8000\r\n"
                    + "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";

            log("SPD Data:"+sdpData);

            byte[] content = sdpData.getBytes();
            request.setContent(content, contentTypeHeader);

            // Send request
            try {
                log(request.toString());
                inviteTid = sipProvider.getNewClientTransaction(request);
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            inviteTid.sendRequest();
                            dialog = inviteTid.getDialog();
                        } catch (SipException e) {
                            e.printStackTrace();
                            log("Error send INVITE request: " + e.getMessage());
                        }
                    }
                };
                thread.start();
            } catch (TransactionUnavailableException e) {
                e.printStackTrace();
                log(e.getMessage());
            }

        } catch (ParseException | InvalidArgumentException e) {
            e.printStackTrace();
            log("Error create INVITE request");
        }
    }

    public void call_established() {
        setState(SipState.IN_CONVERSATION);
        log("Call established!");
    }

    @Override
    public void processRequest(RequestEvent requestEvent) {
        Request request = requestEvent.getRequest();
        log("Got request: " + request.getMethod());
        log(request.toString());

        ServerTransaction st = requestEvent.getServerTransaction();

        if (request.getMethod().equals(Request.BYE)){
            SigmaApp.log("BYE received", TAG);
            if (st == null) {
                SigmaApp.log("Session transaction is Null", TAG);
            } else {
//                Dialog dialog = st.getDialog();
                try {
                    Response response = messageFactory.createResponse(200, request);
                    st.sendResponse(response);
                    SigmaApp.log("OK sent", TAG);
                } catch (ParseException | SipException | InvalidArgumentException e) {
                    e.printStackTrace();
                }
            }
            soundManager.releaseAudioResources();
            mOnSipRequestListener.OnBye();
        } else {
            SigmaApp.log("Unhandled request: " + request.getMethod(), TAG);

        }
    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        Response response = responseEvent.getResponse();
        SigmaApp.log("ResponseCode: " + response.getStatusCode(), TAG);
        SigmaApp.log(response.toString(), TAG);

        Dialog responseDialog;
        ClientTransaction transaction = responseEvent.getClientTransaction();
        CSeqHeader cSeqHeader = (CSeqHeader) response.getHeader(CSeqHeader.NAME);
        if (transaction != null) {
            responseDialog = transaction.getDialog();
        } else {
            responseDialog = responseEvent.getDialog();
        }

        dialog = responseDialog;

        SigmaApp.log("Method: " + cSeqHeader.getMethod(), TAG);

        if (response.getStatusCode() == Response.UNAUTHORIZED ||
                response.getStatusCode() == Response.PROXY_AUTHENTICATION_REQUIRED){
            AuthenticationHelper authenticationHelper = ((SipStackExt) sipStack)
                    .getAuthenticationHelper(new AccountManagerImpl(sipProfile), headerFactory);

            try {
                inviteTid =
                        authenticationHelper.handleChallenge(response, transaction, sipProvider, 5, true);

                inviteTid.sendRequest();
            } catch (SipException e) {
                e.printStackTrace();
            }
        } else if (response.getStatusCode() == Response.OK) {
            if (cSeqHeader.getMethod().equals(Request.INVITE)) {
                try {

                    System.out.println("Sending ACK");
                    Request ackRequest = responseDialog.createAck(cSeqHeader.getSeqNumber());
                    responseDialog.sendAck(ackRequest);

                    byte[] rawContent = response.getRawContent();
                    String sdpContent = new String(rawContent, "UTF-8");
                    SDPAnnounceParser parser = new SDPAnnounceParser(sdpContent);
                    SessionDescriptionImpl sessionDescription = parser.parse();
                    MediaDescription incomingMediaDescription =
                            (MediaDescription) sessionDescription.getMediaDescriptions(false).get(0);
                    int rtpPort = incomingMediaDescription.getMedia().getMediaPort();
                    System.out.println("Invite OK: port " + rtpPort);
                    soundManager.setupRemoteStream(sipProfile.getSrv(), rtpPort);
                    mOnSipRequestListener.OnAnswer();
                } catch (InvalidArgumentException | SipException | UnsupportedEncodingException | ParseException | SdpException e) {
                    e.printStackTrace();
                    log("Error parse INVITE response: " + e.getMessage());
                }
            } else if (cSeqHeader.getMethod().equals(Request.BYE)) {
                SigmaApp.log("BYE OK!", TAG);
                SigmaApp.log("Call dropped!", TAG);
                mOnSipRequestListener.OnFinish();
            } else if ((cSeqHeader.getMethod().equals(Request.CANCEL))) {
                SigmaApp.log("CANCEL OK!", TAG);
                SigmaApp.log("Call dropped!", TAG);
                mOnSipRequestListener.OnFinish();
            }
        }
        else if (
            Arrays.asList(
                    Response.BUSY_HERE,
                    Response.TEMPORARILY_UNAVAILABLE,
                    Response.NOT_IMPLEMENTED
            ).contains(response.getStatusCode())
        ){
            log("Got " + response.getStatusCode() + " interpret as BUSY. End call.");
            drop_call_busy();
        }
        else if (response.getStatusCode() == Response.RINGING) {
            SigmaApp.log("RINGING", TAG);
            mOnSipRequestListener.OnRinging();
        }
        else if (response.getStatusCode() == Response.FORBIDDEN ||
                response.getStatusCode() == Response.SERVICE_UNAVAILABLE) {
            SigmaApp.log("FORBIDDEN or SERVICE_UNAVAILABLE", TAG);
            SigmaApp.log("Callee not answering", TAG);
            setState(SipState.IDLE);
            mOnSipRequestListener.OnFinish();
        }
        else if (response.getStatusCode() == Response.SESSION_PROGRESS) {
            log("Session Progress: " + response.getStatusCode());
            sessionProgress();
        }
        else {
            log("Unhandled response: " + response.getStatusCode());
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {

    }

    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {

    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {

    }


     public static void log(String msg){
        if (WRITE_LOG_TO_FILE) {
            LogHelper.addRecordToLog(msg);
        }
    }

    public enum SipState {
        IDLE,
        CALLING,
        IN_CONVERSATION,
        ERROR_INIT,
        SESSION_PROGRESS //!
    }

    public SipState getState() {
        return state;
    }

    public void setState(SipState state) {
        SigmaApp.log("New call state: " + state, TAG);
        this.state = state;
    }

    public void speakerPhone(boolean on) {
        soundManager.speakerPhone(on);
    }

    public void micMute(boolean on) {
        soundManager.micMute(on);
    }
}
