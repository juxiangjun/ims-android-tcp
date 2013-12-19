package com.eme.ims.audio;


import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.eme.ims.codec.Message;

public class AudioQueuePlayer implements Runnable {
	
	private static AudioTrack audioTrack;
	private static int bufferSize = 0;
	private static AudioQueuePlayer audioQueuePlayer;
	
	public static AudioQueuePlayer getInstance() {
		if (audioQueuePlayer == null) {
			audioQueuePlayer = new AudioQueuePlayer();
		}
		return audioQueuePlayer;
	}
	
	public void putData(Message message, SpeexCodec speex) {
		
		byte[] contents = message.getContents();
//		byte[] tmp = new byte[75];
//		for (int i=0; i<75; i++) {
//			tmp[i] = contents[i];
//		}
		
		for (int i=0; i<32; i++) {
			short[] decodedData = new short[1024];
			byte[] data = Arrays.copyOfRange(contents, i*20, (i+1)*20);
			int size = speex.decode(data, decodedData, 20);
		//	Log.d("", decodedData.toString());
			audioTrack.write(decodedData, 0, size);
		}
		
		
	}
	
	private AudioQueuePlayer() {
		try {
			bufferSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
			audioTrack = new AudioTrack(
					AudioManager.STREAM_MUSIC, 8000, 
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 
					bufferSize /* 1 second buffer */,
					AudioTrack.MODE_STREAM);  
		} catch (Exception e) {
			
		}
	}

	@Override
	public void run() {
		audioTrack.play();
	}
	
	

}
