package de.yovi.chat.user;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;

import de.yovi.chat.ChatUtils.ImageSize;
import de.yovi.chat.api.Friend;
import de.yovi.chat.api.FriendList;
import de.yovi.chat.api.Profile;
import de.yovi.chat.api.ProfileImage;
import de.yovi.chat.api.User;
import de.yovi.chat.communication.PrivateMessageHandler;
import de.yovi.chat.persistence.ImageEntity;
import de.yovi.chat.persistence.PersistenceManager;
import de.yovi.chat.persistence.ProfileEntity;
import de.yovi.chat.persistence.ProfileProvider;
import de.yovi.chat.persistence.UserEntity;

public class ProfileHandler implements ProfileHandlerRemote {

	private final static Logger logger = Logger.getLogger(PrivateMessageHandler.class);

	private final UserProvider userProvider;
	private final ProfileProvider profileProvider;
	
	public ProfileHandler() {
//		EntityManagerFactory emf = Persistence.createEntityManagerFactory("YourChatWeb");

		EntityManagerFactory emf = PersistenceManager.getInstance().getFactory();
		EntityManager entityManager = emf.createEntityManager();
		userProvider = new UserProvider(entityManager);
		profileProvider = new ProfileProvider(entityManager);
	}
	
	
	
	@Override
	public boolean setBirthday(User user, Date birthday) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			if (!luser.isGuest()) {
				UserEntity userEntity = userProvider.getUserEntityByName(luser.getUserName());
				profileProvider.setBirthday(userEntity, birthday != null ? new java.sql.Date(birthday.getTime()) : null);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean setLocation(User user, String location) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			if (!luser.isGuest()) {
				UserEntity userEntity = userProvider.getUserEntityByName(luser.getUserName());
				profileProvider.setLocation(userEntity, location);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean setAbout(User user, String about) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			if (!luser.isGuest()) {
				UserEntity userEntity = userProvider.getUserEntityByName(luser.getUserName());
				profileProvider.setAbout(userEntity, about);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean setGender(User user, int gender)   {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			if (!luser.isGuest()) {
				UserEntity userEntity = userProvider.getUserEntityByName(luser.getUserName());
				profileProvider.setGender(userEntity, gender);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public Long addProfileImage(User user, InputStream uploadStream, String uploadname, String title, String description, boolean asAvatar) {
		LocalUser luser = userProvider.getByUser(user);
		Long newID = null;
		if (luser != null) {
			if (!luser.isGuest()) {
				UserEntity userEntity = userProvider.getUserEntityByName(luser.getUserName());
				if (asAvatar) {
					newID = profileProvider.setAvatar(userEntity, uploadStream, uploadname, title, description);
					luser.setAvatarID(newID);
				} else {
					newID = profileProvider.addGalleryImage(userEntity, uploadStream, uploadname, title, description);
				}
			}
		}
		return newID;
	}
	
	public byte[] getProfileImage(long imageid, ImageSize size) {
		return profileProvider.getImageData(imageid, size);
	}
	
	
	public Profile getProfile(User user, String profileUser) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			UserEntity userEntity = userProvider.getUserEntityByName(profileUser);
			if (userEntity != null) {
				ProfileEntity profileEntity = profileProvider.getProfile(userEntity);
				return new DefaultProfile(userEntity, profileEntity);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	@Override
	public boolean deleteImage(User user, long id) {
		LocalUser luser = userProvider.getByUser(user);
		if (luser != null) {
			UserEntity userEntity = userProvider.getUserEntityByName(user.getUserName());
			ProfileEntity profile = profileProvider.getProfile(userEntity);
			if (profile != null) {
				if (profile.getAvatar() != null && profile.getAvatar().getId() == id) {
					return profileProvider.deleteImage(userEntity, id);
				} else if (profile.getPhotos() != null) {
					boolean doit = false;
					for (ImageEntity photo : profile.getPhotos()) {
						if (photo.getId() == id) {
							doit = true;
						}
					}
					if (doit) {
						return profileProvider.deleteImage(userEntity, id);
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else { 
				return false;
			}
		} else {
			return false;
		}
	}
	
	private class DefaultProfile implements Profile {
		
		private final String name;
		private final Date lastLogin;
		private final int gender;
		private final String about;
		private final String location;
		private final Date birthday;
		private final ProfileImage avatar;
		private final ProfileImage[] collage;;
		
		private DefaultProfile(UserEntity user, ProfileEntity profile) {
			name = user.getName();
			lastLogin = user.getLastlogin();
			gender = profile.getGender();
			about = profile.getAbout();
			location = profile.getLocation();
			birthday = profile.getBirthday();
			final ImageEntity avatarEntity = profile.getAvatar();
			if (avatarEntity != null) {
				avatar = new ProfileImage() {
					
					@Override
					public String getTitle() {
						return avatarEntity.getTitle();
					}
					
					@Override
					public long getID() {
						return avatarEntity.getId();
					}
					
					@Override
					public String getDescription() {
						return avatarEntity.getDescription();
					}
				};
			} else {
				avatar = null;
			}
			Collection<ImageEntity> photos = profile.getPhotos();
			collage = new ProfileImage[photos.size()];
			int i = 0;
			for (final ImageEntity photo : photos) {
				collage[i++] = new ProfileImage() {
					
					@Override
					public String getTitle() {
						return photo.getTitle();
					}
					
					@Override
					public long getID() {
						return photo.getId();
					}

					@Override
					public String getDescription() {
						return photo.getDescription();
					}
				};
			}
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Date getLastLogin() {
			return lastLogin;
		}

		@Override
		public int getGender() {
			return gender;
		}

		@Override
		public String getAbout() {
			return about;
		}

		@Override
		public Date getDateOfBirth() {
			return birthday;
		}

		@Override
		public String getLocation() {
			return location;
		}

		@Override
		public ProfileImage getImage() {
			return avatar;
		}

		@Override
		public ProfileImage[] getCollage() {
			return collage;
		}

		@Override
		public FriendList[] getFriendLists() {
			return new FriendList[] {
					
					new FriendList() {
						
						@Override
						public boolean isVisible() {
							return true;
						}
						
						@Override
						public String getName() {
							return "Beste Freunde";
						}
						
						@Override
						public Friend[] getFriends() {
							return new Friend[] {
									new Friend() {
								
								@Override
								public boolean isOnline() {
									return false;
								}
								
								@Override
								public boolean isConfirmed() {
									return true;
								}
								
								@Override
								public String getUserName() {
									return "Gundula";
								}
								
								@Override
								public Date getLastLogin() {
									return null;
								}
							},
									new Friend() {
										
										@Override
										public boolean isOnline() {
											return true;
										}
										
										@Override
										public boolean isConfirmed() {
											return true;
										}
										
										@Override
										public String getUserName() {
											return "Werner";
										}
										
										@Override
										public Date getLastLogin() {
											return null;
										}
									}
							};
							}
						}
					};
			}
		
		
	}
	
	
}
