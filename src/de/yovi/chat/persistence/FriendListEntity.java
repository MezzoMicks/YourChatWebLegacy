package de.yovi.chat.persistence;

import java.util.Collection;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "friendlist")
public class FriendListEntity {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private long id;
	@Basic
	@Column(length=40)
	private String name;
	@Basic
	private boolean visible;
	@OneToMany
	@JoinColumn(name = "friendlist_id", referencedColumnName = "id")
	private Collection<FriendListUserEntity> friendListUserEntity;
	public long getId() {
		return id;
	}

	public void setId(long param) {
		this.id = param;
	}

	public void setName(String param) {
		this.name = param;
	}

	public String getName() {
		return name;
	}

	public void setVisible(boolean param) {
		this.visible = param;
	}

	public boolean getVisible() {
		return visible;
	}

	public Collection<FriendListUserEntity> getFriendListUserEntity() {
	    return friendListUserEntity;
	}

	public void setFriendListUserEntity(Collection<FriendListUserEntity> param) {
	    this.friendListUserEntity = param;
	}

}