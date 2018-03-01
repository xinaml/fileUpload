package com.changbei.modules.storage.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.changbei.common.persistence.BaseEntity;
import com.changbei.modules.storage.type.FileType;

@Entity
@Table(name = "tb_storage_file")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class File extends BaseEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	/**
	 *关联id ,通过该id可找到对应文件
	 */
	private Long relevanceId;
	

    /**
     * 所属用户
     */
    private Long userId;
	
	/**
     * 文件名
     */
    private String name;

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件大小
     */
    private String size;
    /**
     * 是否可下载
     */
    private Boolean down;
    /**
     * 文件md5
     */
    private String md5;


    public Boolean getDown() {
		return down;
	}

	public void setDown(Boolean down) {
		this.down = down;
	}

	/**
     * 创建时间
     */
	private Date createDate;


    public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getRelevanceId() {
		return relevanceId;
	}

	public void setRelevanceId(Long relevanceId) {
		this.relevanceId = relevanceId;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

}
