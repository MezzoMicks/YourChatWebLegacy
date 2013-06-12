package de.yovi.chat.persistence;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Entity implementation class for Entity: User
 *
 */
@Entity
@Table(name="chatuser")
@NamedQueries({
	@NamedQuery(name="getUserByName", 
				query="SELECT u " //
					+ "FROM UserEntity u " //
					+ "WHERE LOWER(u.name) = :name"//
				),
	@NamedQuery(name="findUserNamesByName", 
	query="SELECT u.name " //
		+ "FROM UserEntity u " //
		+ "WHERE LOWER(u.name) like :name"//
	)
})
public class UserEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	   
	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private long id;
	@Basic
	@Column(length=20)
	private String name;
	@Basic
	@Column(length=64)
	private String password;
	@Basic
	@Column(length=8)
	private String color;
	@Basic
	@Column(length=20)
	private String font;
	@Basic
	private boolean trusted;
	@Basic
	@Column(length=20)
	private String room;
	@Basic
	private Timestamp lastlogin;
	@OneToOne
	private ProfileEntity profile;
	@Basic
	private boolean asyncmode;
	public UserEntity() {
		super();
	}   
	public long getId() {
		return this.id;
	}

	public void setId(long param) {
		this.id = param;
	}   
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}   
	public String getPassword() {
		return this.password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getColor() {
		return color;
	}
	public void setColor(String color) {
		this.color = color;
	}
	public String getFont() {
		return font;
	}
	public void setFont(String font) {
		this.font = font;
	}
	public boolean isTrusted() {
		return trusted;
	}
	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}
	public Timestamp getLastlogin() {
		return lastlogin;
	}
	public void setLastlogin(Timestamp lastLogin) {
		this.lastlogin = lastLogin;
	}
	public ProfileEntity getProfile() {
	    return profile;
	}
	public void setProfile(ProfileEntity param) {
	    this.profile = param;
	}
	public String getRoom() {
		return room;
	}
	public void setRoom(String room) {
		this.room = room;
	}
	public boolean isAsyncmode() {
		return asyncmode;
	}
	public void setAsyncmode(boolean asyncmode) {
		this.asyncmode = asyncmode;
	}
   
}
