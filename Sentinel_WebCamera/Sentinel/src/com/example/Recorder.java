package com.example;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.WebcamUtils;
import com.github.sarxos.webcam.util.ImageUtils;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class Recorder {

	private volatile boolean mIsRecording;
	private SimpleDateFormat mDateFormat;
	
	public Recorder() {
		super();
		mDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
	}
	
	public void recordImageJpg(Webcam aWebcam, String aFileName) {
		File file = new File(aFileName + ".jpg");
		int unique = 2;
		while(file.exists()) {
			aFileName = aFileName + "_" + unique;
			unique++;
			file = new File(aFileName + ".jpg");
		}
		WebcamUtils.capture(aWebcam, aFileName, ImageUtils.FORMAT_JPG);
	}
	
	public void recordStart(Webcam aWebcam, String aFileName) {
		File file = new File(aFileName + ".mpeg");
		int unique = 2;
		while(file.exists()) {
			aFileName = aFileName + "_" + unique;
			unique++;
			file = new File(aFileName + ".mpeg");
		}
		
		IMediaWriter writer = ToolFactory.makeWriter(file.getName());
		Dimension size = WebcamResolution.VGA.getSize();
		//writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);
		//writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG1VIDEO, size.width, size.height);
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG2VIDEO, size.width, size.height);

		long start = System.currentTimeMillis();
		mIsRecording = true;
		int keyFrame = 0;
		Date date = null;
		String time = null;
		while(mIsRecording) {	
			BufferedImage image = ConverterFactory.convertToType(aWebcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
			IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
			
			date = new Date();
			time = mDateFormat.format(date);
			Graphics graphics = image.getGraphics();
			graphics.drawString(time, 20, 30);
			graphics.dispose();
			
			IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
			frame.setKeyFrame(keyFrame == 0);
			frame.setQuality(0);
			
			writer.encodeVideo(0, frame);
			
			//25 FPS
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			keyFrame++;
			if(keyFrame > 1) {
				keyFrame = 0;
			}
		}
		writer.close();
	}
	
	public void recordStop() {
		mIsRecording = false;
	}
	
	public boolean isRecording() {return mIsRecording;}
}
