package com.eme.ims.audio;


import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.eme.ims.codec.Message;

public class AudioQueuePlayer implements Runnable {
	
	private static AudioTrack audioTrack;
	private static int bufferSize = 0;
	private static AudioQueuePlayer audioQueuePlayer;
	private static int SPEEX_DATA_SIZE = 20;
	
	public static AudioQueuePlayer getInstance() {
		if (audioQueuePlayer == null) {
			audioQueuePlayer = new AudioQueuePlayer();
		}
		return audioQueuePlayer;
	}
	
	public void putData(Message message, SpeexCodec speex) {
		
		byte[] contents = message.getContents();

		int packages = contents.length / SPEEX_DATA_SIZE;
		
		for (int i=0; i<packages; i++) {
			short[] decodedData = new short[1024];
			byte[] data = Arrays.copyOfRange(contents, i*SPEEX_DATA_SIZE, (i+1)*SPEEX_DATA_SIZE);
			if (data[0] == 0x0029) {
				continue;
			}
			int size = speex.decode(data, decodedData, SPEEX_DATA_SIZE);
			audioTrack.write(decodedData, 0, size);
		}
		
//		short[] decodedData = new short[4096];
//		int size = speex.decode(contents, decodedData, contents.length);
//		audioTrack.write(decodedData, 0, size);
		
	}
	
	private AudioQueuePlayer() {
		try {
			bufferSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
			audioTrack = new AudioTrack(
					AudioManager.STREAM_VOICE_CALL, 8000, 
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
