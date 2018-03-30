package com.example;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

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
	
	public Recorder() {
		super();
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
		File file = new File(aFileName + ".ts");
		int unique = 2;
		while(file.exists()) {
			aFileName = aFileName + "_" + unique;
			unique++;
			file = new File(aFileName + ".ts");
		}
		
		IMediaWriter writer = ToolFactory.makeWriter(file.getName());
		Dimension size = WebcamResolution.VGA.getSize();
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264, size.width, size.height);

		long start = System.currentTimeMillis();
		mIsRecording = true;
		int keyFrame = 0;
		while(mIsRecording) {	
			BufferedImage image = ConverterFactory.convertToType(aWebcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
			IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
			
			IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis() - start) * 1000);
			frame.setKeyFrame(keyFrame == 0);
			frame.setQuality(0);
			
			writer.encodeVideo(0, frame);
			
			//20 FPS
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			keyFrame++;
		}
		writer.close();
	}
	
	public void recordStop() {
		mIsRecording = false;
	}
	
	public boolean isRecording() {return mIsRecording;}
}
