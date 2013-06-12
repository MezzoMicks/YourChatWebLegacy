package de.yovi.chat.persistence;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "friendlistuser")
public class FriendListUserEntity {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private long id;
	@Basic
	private boolean confirmed;
	@OneToOne
	private UserEntity userEntity;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public boolean isConfirmed() {
		return confirmed;
	}
	
	public void setConfirmed(boolean param) {
		this.confirmed = param;
	}

	public UserEntity getUserEntity() {
	    return userEntity;
	}

	public void setUserEntity(UserEntity param) {
	    this.userEntity = param;
	}

}