package de.yovi.chat.system;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.web.Configurator;

public class FileStorage {
	
	private final static Logger logger = Logger.getLogger(FileStorage.class);
	private final static int BLOCK_SIZE = 16;
	private static FileStorage instance = null;
	
	private final File dataDir;
	private final SecretKey secKey;
	
	private final boolean safeMode;
	
	private FileStorage(boolean safeMode) {
		this.safeMode = safeMode;
		Cipher newEnCipher = null;
		Cipher newDeCipher = null;
		SecretKey newSecKey = null;
		if (safeMode) {
			logger.info("Initializing FileStorage in SAFE-Mode");
			try {
				newEnCipher = Cipher.getInstance("AES");
				newDeCipher = Cipher.getInstance("AES");
				KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	//			keyGen.init(256);
				newSecKey = keyGen.generateKey();
				// Test if Ciphers can be created
				newEnCipher.init(Cipher.ENCRYPT_MODE, newSecKey);
				newDeCipher.init(Cipher.DECRYPT_MODE, newSecKey);
			} catch (Exception e) {
				logger.error(e);
				newSecKey = null;
			}
		} else {
			logger.info("Initializing FileStorage in UNsafe-Mode");
		}
		dataDir = Configurator.getDataFile();
		secKey = newSecKey;
	}
	
	public static FileStorage getInstance() {
		if (instance == null) {
			createInstance();
		}
		return instance;
	}
	
	private static synchronized void createInstance() {
		if (instance == null) {
			instance = new FileStorage(true);
		}
	}
	
	/**
	 * Encrypts the given InputStream into the given OutputStream
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public void encryptAndCopy(InputStream is, OutputStream os) throws IOException {
		if (secKey == null) {
			if (safeMode) {
				logger.warn("Fallback to unsafemode, due to lacking Key!");
			}
			IOUtils.copy(is, os);
		} else {
			long start = System.currentTimeMillis();
			CipherOutputStream cos = new CipherOutputStream(os, getCipher(Cipher.ENCRYPT_MODE));
			byte[] block = new byte[BLOCK_SIZE];
			int i;
			while ((i = is.read(block)) != -1) {
				cos.write(block, 0, i);
			}
			cos.close();
			if (logger.isDebugEnabled()) {
				logger.debug("Encryption took " + (System.currentTimeMillis() - start) + "ms");
			}
		}
	}
	
	/**
	 * Decrypts the given InputStream to the given OutputStream
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public void decryptAndCopy(InputStream is, OutputStream os) throws IOException {
		if (secKey == null) {
			if (safeMode) {
				logger.warn("Fallback to unsafemode, due to lacking Key!");
			}
			IOUtils.copy(is, os);
		} else {
			long start = System.currentTimeMillis();
			CipherInputStream cis = new CipherInputStream(is, getCipher(Cipher.DECRYPT_MODE));
			byte[] block = new byte[BLOCK_SIZE];
			int i;
			while ((i = cis.read(block)) != -1) {
				os.write(block, 0, i);
			}
			cis.close();
			if (logger.isDebugEnabled()) {
				logger.debug("Decryption took " + (System.currentTimeMillis() - start) + "ms");
			}
		}
	}
	
	/**
	 * Stores the given InputStream's Data into a File with the given name (or a similiar one)
	 * @param is
	 * @param name
	 * @return the Name the File eventually retrieved
	 */
	public String store(InputStream is, String name) {
		logger.info("storing " + name);
		File targetFile = new File(dataDir, name);
		String extName;
		String mainName;
		int lastIxOfDot = name.lastIndexOf('.');
		if (lastIxOfDot < 0) {
			extName = "";
			mainName = name;
		} else {
			extName = name.substring(lastIxOfDot);
			mainName = name.substring(0, lastIxOfDot);
		}
		int i = 0;
		while (targetFile.exists()) {
			targetFile = new File(dataDir, mainName + i++ + extName);
		}
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("storing " + name + " to " + targetFile.getAbsolutePath());
			}
			targetFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(targetFile);
			encryptAndCopy(is, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			logger.error("error while storing " + name + " to " + targetFile.getAbsolutePath(), e);
		} catch (IOException e) {
			logger.error("error while storing " + name + " to " + targetFile.getAbsolutePath(), e);
		}
		return targetFile.getName();
	}
	
	/**
	 * Loads a File to the OutputStream by it's name
	 * @param os
	 * @param name
	 */
	public boolean load(OutputStream os,String name) {
		logger.info("loading " + name);
		File sourceFile = new File(dataDir, name);
		boolean success = false;
		try {
			logger.debug("loading " + name + " from " + sourceFile.getName());
			FileInputStream fis = new FileInputStream(sourceFile);
			decryptAndCopy(fis, os);
			fis.close();
			success = true;
		} catch (FileNotFoundException e) {
			logger.error("error while loading " + name + " to " + sourceFile.getAbsolutePath(), e);
		} catch (IOException e) {
			logger.error("error while loading " + name + " to " + sourceFile.getAbsolutePath(), e);
		}
		return success;
	}
	
	private Cipher getCipher(int mode) {
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance("AES");
			cipher.init(mode, secKey);
		} catch (Exception e) {
			logger.error("This shouldn't be!", e);
		}
		return cipher;
	}
	
	public static void main(String[] args) throws Exception {
		new Configurator().debug();
		Provider[] providers = Security.getProviders();
		for (Provider provider : providers) {
			System.out.println(provider.getName());
			Set<Service> services = provider.getServices();
			for (Service service : services) {
				System.out.println(" - " + service.getAlgorithm());
				
			}
		}

		String cleartextFile = "clearpic.png";

		FileInputStream fis = new FileInputStream(cleartextFile);
		FileStorage fs = new FileStorage(true);
		String newName = fs.store(fis, "clearpic.png");
		
		String cleartextAgainFile = "clearpicagain.png";
		FileOutputStream fos = new FileOutputStream(cleartextAgainFile);
		fs.load(fos, newName);
		

	}

}