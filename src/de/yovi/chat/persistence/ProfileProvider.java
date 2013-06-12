package de.yovi.chat.persistence;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Date;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import de.yovi.chat.ChatUtils;
import de.yovi.chat.ChatUtils.ImageSize;

public class ProfileProvider {
	
	private final static Logger logger = Logger.getLogger(ProfileProvider.class);
	private final PersistenceManager pm = PersistenceManager.getInstance();
	private final EntityManager em;
	
	public ProfileProvider(EntityManager em) {
		this.em = em;
	}
	
	public byte[] getImageData(long id, ImageSize size) {
		ImageEntity imageEntity = em.find(ImageEntity.class, id);
		if (imageEntity != null) {
			switch (size) {
			default:
			case ORIGINAL:
				return imageEntity.getOriginal();
			case PREVIEW:
				return imageEntity.getPreview();
			case THUMBNAIL:
				return imageEntity.getThumbnail();
			case PINKY:
				return imageEntity.getPinkynail();
			}
		} else {
			return null;
		}
	}
	
	public boolean deleteImage(UserEntity user, long id) {
		if (user != null) {
			ImageEntity imageEntity = em.find(ImageEntity.class, id);
			if (imageEntity != null) {
				ProfileEntity profileEntity = user.getProfile();
				profileEntity.getPhotos().remove(imageEntity);
				logger.info("removing image " + id + " from profile");
				pm.persistOrMerge(em, profileEntity, false);
				logger.info("removing image " + id + " from db");
				pm.remove(em, imageEntity);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public ProfileEntity getProfile(UserEntity user) {
		ProfileEntity profile = user.getProfile();
		if (profile == null) {
			profile = new ProfileEntity();
			profile.setGender(0);
			profile.setAbout("");
			pm.persistOrMerge(em, profile, true);
			user.setProfile(profile);
			pm.persistOrMerge(em, user, false);
		}
		return profile;
	}
	
	public boolean setGender(UserEntity user, int gender) {
		ProfileEntity profile = getProfile(user);
		profile.setGender(gender);
		pm.persistOrMerge(em, profile, false);
		return true;
	}

	public boolean setBirthday(UserEntity user, Date birthday) {
		ProfileEntity profile = getProfile(user);
		profile.setBirthday(birthday);
		pm.persistOrMerge(em, profile, false);
		return true;
	}
	
	public boolean setLocation(UserEntity user, String location) {
		ProfileEntity profile = getProfile(user);
		profile.setLocation(location);
		pm.persistOrMerge(em, profile, false);
		return true;
	}
	
	public boolean setAbout(UserEntity user, String about) {
		ProfileEntity profile = getProfile(user);
		profile.setAbout(about);
		pm.persistOrMerge(em, profile, false);
		return true;
	}
	
	public Long setAvatar(UserEntity user, InputStream uploadStream, String uploadName, String title, String description) {
		logger.info("setting avatar for userprofile " + user);
		ProfileEntity profile = getProfile(user);
		ImageEntity newImage = createImageEntity(uploadStream, uploadName, title, description);
		if (newImage != null) {
			ImageEntity oldImage = profile.getAvatar();
			profile.setAvatar(newImage);
			pm.persistOrMerge(em, profile, false);
			if (oldImage != null) {
				pm.remove(em, oldImage);
			}
			return newImage.getId();
		} else {
			return null;
		}
	}
	
	public Long addGalleryImage(UserEntity user, InputStream uploadStream, String uploadName, String title, String description) {
		logger.info("adding image to userprofile " + user);
		ProfileEntity profile = getProfile(user);
		ImageEntity newImage = createImageEntity(uploadStream, uploadName, title, description);
		if (newImage != null) {
			profile.getPhotos().add(newImage);
			pm.persistOrMerge(em, profile, false);
			return newImage.getId();
		} else {
			return null;
		}
	}

	private ImageEntity createImageEntity(InputStream uploadStream, String uploadname, String title, String description) {
		final ImageEntity newEntity = new ImageEntity();
		try {
			// Get Name, Title and Description
			newEntity.setFilename(uploadname);
			newEntity.setTitle(title);
			newEntity.setDescription(description);
			logger.info("creating imageentity for file " + uploadname);
			// store the original
//			final byte[] originalAsByte = new byte[(int) file.length()];
	        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			IOUtils.copy(uploadStream, baos);
			final byte[] originalAsByte = baos.toByteArray();
//			FileInputStream fis = new FileInputStream(file);
			newEntity.setOriginal(originalAsByte);
			// make a Image for the file
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalAsByte));
			// create preview (280) and store it
	        baos.reset();
	        int previewSize = ImageSize.PREVIEW.getSize();
	        ChatUtils.createResized(image, baos, previewSize, previewSize, null);
	        newEntity.setPreview(baos.toByteArray());
	        baos.reset();
	        // create thumbnail (160) and store it
	        int thumbSize = ImageSize.THUMBNAIL.getSize();
	        ChatUtils.createResized(image, baos, thumbSize, thumbSize, null);
	        newEntity.setThumbnail(baos.toByteArray());
	        baos.reset();
	        // create pinkynail (64) and store it
	        int pinkySize = ImageSize.PINKY.getSize();
	        ChatUtils.createResized(image, baos, pinkySize, pinkySize, null);
	        newEntity.setPinkynail(baos.toByteArray());
	        baos.close();
	        // write to db
			PersistenceManager.getInstance().persistOrMerge(em, newEntity, true);
			return newEntity;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}
	
}
