package ru.sigmacall.sigma.sip;

import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SoundManager {

    SipStack sipStack;
    Context appContext;

    AudioManager audioManager;
    AudioStream audioStream;
    AudioGroup audioGroup;

    public SoundManager(Context context, String localIp, SipStack sipStack) {
        this.appContext = context;
        this.sipStack = sipStack;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        try {
            audioStream = new AudioStream(InetAddress.getByName(localIp));
            audioStream.setCodec(AudioCodec.PCMU);
            audioStream.setMode(RtpStream.MODE_NORMAL);

            audioGroup = new AudioGroup();
            audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);

        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            SipStack.log("Error AudioStream: " + e.getMessage());
        }
    }

    public int setupLocalStream(){
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        return audioStream.getLocalPort();
    }

    public void setupRemoteStream(String rIp, int rPort) {
        try {
            audioStream.associate(
                    InetAddress.getByName(rIp),
                    rPort
            );
        } catch (UnknownHostException e) {
            e.printStackTrace();
            SipStack.log("Error setup remote stream: " + e.getMessage());
        }
        audioStream.join(audioGroup);
        sipStack.call_established();
    }

    public void releaseAudioResources() {
        audioStream.join(null);
        audioGroup.clear();
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public void releaseAudioResourcesFull() {
        audioStream.join(null);
        audioStream.release();
        audioGroup.clear();
        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public void speakerPhone(boolean on) {
        audioManager.setSpeakerphoneOn(on);
    }

    public void micMute(boolean on) {
        audioManager.setMicrophoneMute(on);
    }
}
