package de.yovi.chat.persistence;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "image")
public class ImageEntity {

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	private long id;
	@Basic
	@Column(length=255)
	private String title;
	@Basic
	@Column(length=4000)
	private String description;
	@Basic
	@Column(length=255)
	private String filename;
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] original;
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] preview;
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] thumbnail;
	@Basic(fetch = FetchType.LAZY)
	@Lob
	private byte[] pinkynail;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public byte[] getOriginal() {
		return original;
	}
	public void setOriginal(byte[] original) {
		this.original = original;
	}
	public byte[] getPreview() {
		return preview;
	}
	public void setPreview(byte[] preview) {
		this.preview = preview;
	}
	public byte[] getThumbnail() {
		return thumbnail;
	}
	public void setThumbnail(byte[] thumbnail) {
		this.thumbnail = thumbnail;
	}
	public byte[] getPinkynail() {
		return pinkynail;
	}
	public void setPinkynail(byte[] pinkynail) {
		this.pinkynail = pinkynail;
	}
}