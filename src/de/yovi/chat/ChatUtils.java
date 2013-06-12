package de.yovi.chat;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import de.yovi.chat.system.FileStorage;

public class ChatUtils {
	

	public enum ImageSize {
		ORIGINAL("", -1), PREVIEW("preview_", 280), THUMBNAIL("thumb_", 160), PINKY("pinky_", 64),;
		
		private final String prefix;
		private final int size;
		
		private ImageSize(String prefix, int size) {
			this.size = size;
			this.prefix = prefix;
		}
		
		public String getPrefix() {
			return prefix;
		}
		
		public int getSize() {
			return size;
		}
	}
	
	private final static Logger logger = Logger.getLogger(ChatUtils.class);


	private final static char[] GOOD_ONES = new char[] {
		'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
		'0','1','2','3','4','5','6','7','8','9','0','.','-','_','+'
	};
	
	private static boolean isGoodOne(char ch) {
		for (char good : GOOD_ONES) {
			if (Character.toLowerCase(ch) == good) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Replaces the special characters inside a String with underscores '_' or a matching substitue 'ä'="ae"
	 * @param input
	 * @return replaced string
	 */
	public static String replaceSpecialChars(String input) {
		char[] result = new char[input.length()];
		char[] inputChars = input.toCharArray();
		int length = inputChars.length;
		for (int i = 0; i < length; i++) {
			char ch = inputChars[i];
			if (isGoodOne(ch)) {
				result[i] = ch;
			} else if (ch == 'ä') {
				result[i++] = 'a';
				result[i] = 'e';
				length++;
			} else if (ch == 'Ä') {
				result[i++] = 'A';
				result[i] = 'E';
				length++;
			} else if (ch == 'ö') {
				result[i++] = 'o';
				result[i] = 'e';
				length++;
			} else if (ch == 'Ö') {
				result[i++] = 'O';
				result[i] = 'E';
				length++;
			} else if (ch == 'ü') {
				result[i++] = 'u';
				result[i] = 'e';
				length++;
			} else if (ch == 'Ü') {
				result[i++] = 'U';
				result[i] = 'E';
				length++;
			} else if (ch == 'ß') {
				result[i++] = 's';
				result[i] = 's';
				length++;
			} else {
				result[i] = '_';
			}
		}
		return new String(result);
	}
	
	public static String createAndStoreResized(String prefix, InputStream stream, String filename, int width, int height, Color backdrop) throws IOException {
		logger.info("Creating Thumbnail for " + filename);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ChatUtils.createResized(stream, bos, width, height, null);
		return store(prefix, filename, bos);
	}
	
	public static String createAndStoreResized(String prefix, BufferedImage image, String filename, int width, int height, Color backdrop) throws IOException {
		logger.info("Creating Thumbnail for " + filename);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ChatUtils.createResized(image, bos, width, height, null);
		return store(prefix, filename, bos);
	}

	private static String store(String prefix, String filename, ByteArrayOutputStream bos) {
		ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
		String name = ChatUtils.replaceSpecialChars(filename != null ? filename : "unknown");
		int lastIxOfDot = name.lastIndexOf('.');
		if (lastIxOfDot >= 0) {
			name = name.substring(0, lastIxOfDot);
		}
		name += ".jpg";
		name = FileStorage.getInstance().store(bis, prefix + name);
		return name;
	}

	/**
	 * Creates a thumbnail for an image, resulting in an imagefile of reduced size and lower quality
	 * @param url
	 * @param target
	 * @param width
	 * @param height
	 */
	public static void createResized(InputStream input, OutputStream output, int targetWidth, int targetHeight, Color backdrop) {
		try {
			ImageInputStream iios = ImageIO.createImageInputStream(input);
			BufferedImage src = ImageIO.read(iios);
			createResized(src, output, targetWidth, targetHeight, backdrop);
		} catch (IOException e) {
			logger.error(e);
		}
	}
	
	public static void createResized(BufferedImage image, OutputStream output, int targetWidth, int targetHeight, Color backdrop) {
		try {
			if (image != null) {
				int width = image.getWidth();
				int height = image.getHeight();
				int[] thumbsize = getThumbsize(width, height, targetWidth, targetHeight);
				if (targetWidth < 0) {
					targetWidth = thumbsize[0];
				} 
				if (targetHeight < 0) {
					targetHeight = thumbsize[1];
				}
				logger.debug("Instanciating thumbnail");
				BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
				// draw original image to thumbnail image object and
				// scale it to the new size on-the-fly
				logger.debug("creating 'graphics' for thumbnail");
				Graphics2D graphics2D = thumbnail.createGraphics();
				graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				graphics2D.setColor(backdrop != null ? backdrop : Color.WHITE);
				logger.debug("filling with white");
				graphics2D.fillRect(0, 0, targetWidth, targetHeight);
				logger.debug("drawing actual image to thumbnail");
				graphics2D.drawImage(image, thumbsize[2], thumbsize[3], thumbsize[0], thumbsize[1], null);
				// Compress the Image to an lower quality JPEG
				logger.debug("Preparing JPEG-ImageWriter");
				Iterator<ImageWriter> i = ImageIO.getImageWritersByFormatName("jpeg");
				// Just get the first JPEG writer available
				ImageWriter writer = i.next();
				// Set the compression quality to 0.75
				ImageWriteParam param = writer.getDefaultWriteParam();
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.75f);

				// Write the image to a file
				ImageOutputStream ios = ImageIO.createImageOutputStream(output);
				writer.setOutput(ios);
				writer.write(thumbnail);
				writer.dispose();
				ios.close();
			} else {
				logger.warn("No image supplied! null!");
			}
		} catch (IOException e) {
			logger.error("Error while creating thumbnail", e);
		}
	}

	/**
	 * Lays an transparent overlay over the Image
	 * Snippet: thanks to Josiah Hester on javalobby.org
	 * @param upload
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void makeImageMoreTransparent(InputStream input, OutputStream output, Color backdrop) throws IOException {
		BufferedImage bgImage;
		bgImage = ImageIO.read(input);
		BufferedImage newImg = new BufferedImage(bgImage.getWidth(), bgImage.getHeight(), BufferedImage.TYPE_INT_RGB);  
		// Get the images graphics  
		Graphics2D g = newImg.createGraphics();  
		if (backdrop != null) {
			g.setColor(backdrop);
			g.fillRect(0, 0, bgImage.getWidth(), bgImage.getHeight());
		}
		// Set the Graphics composite to Alpha  
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));  
		// Draw the LOADED img into the prepared receiver image  
		g.drawImage(bgImage, null, 0, 0);  
		// let go of all system resources in this Graphics  
		g.dispose();
		ImageOutputStream ios = ImageIO.createImageOutputStream(output);
		ImageIO.write(newImg, "png", ios);
	    ios.close();
	}
	
	public static String escape(String input) {
		if (input == null) {
			return null;
		} else {
			input = StringEscapeUtils.escapeHtml(input);
			input = input.replace("\n", "<br/>");
			return input;
		}
	}
	
	public static int calculateAge(Date birthday) {
		Calendar cal = Calendar.getInstance();
		int thisYear = cal.get(Calendar.YEAR);
		int thisDoY = cal.get(Calendar.DAY_OF_YEAR);
		cal.setTime(birthday);
		int birthYear = cal.get(Calendar.YEAR);
		int birthDoY = cal.get(Calendar.DAY_OF_YEAR);
		int age = thisYear - birthYear;
		if (thisDoY < birthDoY) {
			age -= 1;
		}
		return age;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
//		FileInputStream fis = new FileInputStream("D:\\asukareiagainmo1.png");
//		FileOutputStream fos = new FileOutputStream("D:\\thumb_asukareiagainmo1.JPG");
//		createResized(fis, fos, 160, 160);
//		System.out.println("FETTIG!");
//	
		int[] thumb;
		thumb = getThumbsize(400, 300, 1000, 600);
		System.out.println(thumb[0] + "|" + thumb[1] + "|" + thumb[2] + "|" + thumb[3]);
		thumb = getThumbsize(400, 400, 1000, 600);
		System.out.println(thumb[0] + "|" + thumb[1] + "|" + thumb[2] + "|" + thumb[3]);
		thumb = getThumbsize(400, 300, 800, -1);
		System.out.println(thumb[0] + "|" + thumb[1] + "|" + thumb[2] + "|" + thumb[3]);
		thumb = getThumbsize(400, 300, -1, 600);
		System.out.println(thumb[0] + "|" + thumb[1] + "|" + thumb[2] + "|" + thumb[3]);
		thumb = getThumbsize(400, 300, 180, 180);
		System.out.println(thumb[0] + "|" + thumb[1] + "|" + thumb[2] + "|" + thumb[3]);
	}
	
	/**
	 * Calculates the size of a thumbnail and it's position within the canvas, so that it has a proper alignment (centered)
	 * @param width
	 * @param height
	 * @param maxWidth
	 * @param maxHeight
	 * @return int[] 0 = width; 1 = height; 2 = posX; 3 = posY
	 */
	private static int[] getThumbsize(int width, int height, int maxWidth, int maxHeight) {
		if (width == height) {
			return new int[] { maxWidth, maxHeight, 0, 0};
		} else {
			// proportional rescale
			if (maxWidth == -1 || maxHeight == -1) {
				// scale over width
				if (maxWidth > 0) {
					height = (int) ((maxWidth / (double) width) * height);
					width = maxWidth;
				// scale over height
				} else if (maxHeight > 0) {
					width = (int) ((maxHeight / (double) height) * width);
					height = maxHeight;
				} // no scale? if both <= 0
				return new int[] {width, height, 0, 0};
			// full rescale in canvas
			} else {
				int bigSide;
				int smallSide;
				int biggerMax;
				int smallerMax;
				// figure which is bigger
				boolean widthIsBigger = width > height;
				// move the values to their proper variables
				if (widthIsBigger) {
					if (((maxWidth / (double) width) * height) > maxHeight) {
						widthIsBigger = false;
					}
				}
				if (widthIsBigger) {
					biggerMax = maxWidth;
					smallerMax = maxHeight;
					bigSide = width;
					smallSide = height;
				} else {
					biggerMax = maxHeight;
					smallerMax = maxWidth;
					bigSide = height;
					smallSide = width;
				}
				// calculate the smaller sides new size
				int newSmallSide = (int) ((biggerMax * smallSide) / bigSide);
				int position = (int) ((smallerMax - newSmallSide) / 2);
				// Return the Values at their proper indicies
				if (widthIsBigger) {
					return new int[] {biggerMax, newSmallSide, 0, position};
				} else {
					return new int[] {newSmallSide, biggerMax, position, 0};
				}
			}
//			if (width > height) {
//				newWidth = maxWidth;
//				newHeight = (int) ((newWidth * height) / width);
//	
//				newPosX = 0;
//				newPosY = (int) ((maxHeight - newHeight) / 2);
//			} else {
//				newHeight = maxHeight;
//				newWidth = (int) ((newHeight * width) / height);
//	
//				newPosX = (int) ((maxWidth - newWidth) / 2);
//				newPosY = 0;
//			}
//	
//			return new int[] { newWidth,  newHeight, newPosX, newPosY };
		}
	}
	
}
