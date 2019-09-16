package com.example;

import java.io.IOException;

public class Sentinel {
	
	public static void main(String[] args) throws IOException {
		SentinelWorker sentinel = new SentinelWorker();
		sentinel.createGui();
	}
}
