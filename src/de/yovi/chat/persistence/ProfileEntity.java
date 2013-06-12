package de.yovi.chat.persistence;

import java.sql.Date;
import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "profile")
public class ProfileEntity {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private long id;
	@Basic
	private int gender;
	@Basic
	private Date birthday;
	@Basic
	@Column(length=40)
	private String location;
	@Basic
	@Column(length=40)
	private String oneliner;
	@Basic(fetch = FetchType.LAZY)
	@Column(length=4000)
	private String about;
	@OneToOne
	private ImageEntity avatar;
	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "profile_id", referencedColumnName = "id")
	private Collection<ImageEntity> photos;
	@OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "profile_id", referencedColumnName = "id")
	private Collection<FriendListEntity> friendList;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setGender(int param) {
		this.gender = param;
	}

	public int getGender() {
		return gender;
	}
	
	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
	
	public Date getBirthday() {
		return birthday;
	}
	
	public void setOneliner(String oneliner) {
		this.oneliner = oneliner;
	}
	
	public String getOneliner() {
		return oneliner;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public String getLocation() {
		return location;
	}

	public void setAbout(String param) {
	    this.about = param;
	}

	public String getAbout() {
	    return about;
	}

	public ImageEntity getAvatar() {
	    return avatar;
	}

	public void setAvatar(ImageEntity param) {
	    this.avatar = param;
	}

	public Collection<ImageEntity> getPhotos() {
	    return photos;
	}

	public void setPhotos(Collection<ImageEntity> param) {
	    this.photos = param;
	}

	public Collection<FriendListEntity> getFriendList() {
	    return friendList;
	}

	public void setFriendList(Collection<FriendListEntity> param) {
	    this.friendList = param;
	}

}