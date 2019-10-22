package com.quadcore.naada;

import java.io.File;
import java.util.ArrayList;

public class SongListManager {
	ArrayList<File> songsList = new ArrayList<File>();

	ArrayList<File> getPlayList() {
		return songsList;
	}

	public SongListManager() {
		String PATH = "/sdcard";
		File home = new File(PATH);
		// String MEDIA_PATH = new
		// String(MediaStore.Audio.Media.getContentUri("external").toString());

		File[] listFiles = home.listFiles();
		if (listFiles != null && listFiles.length > 0) {
			for (File file : listFiles) {
				// Adding each song to SongList
				if (file.isDirectory()) {
					scanDirectory(file);
				} else if (file.getName().endsWith(".mp3")
						|| file.getName().endsWith(".wav")
						|| file.getName().endsWith(".m4a")
						|| file.getName().endsWith(".MP3")) {
					songsList.add(file);
				}
			}
		}
	}

	private void scanDirectory(File directory) {
		if (directory != null) {
			File[] listFiles = directory.listFiles();
			if (listFiles != null && listFiles.length > 0) {
				for (File file : listFiles) {
					if (file.isDirectory()) {
						scanDirectory(file);
					} else if (file.getName().endsWith(".mp3")
							|| file.getName().endsWith(".wav")
							|| file.getName().endsWith(".m4a")
							|| file.getName().endsWith(".MP3")) {
						songsList.add(file);
					}

				}
			}
		}
	}
}
