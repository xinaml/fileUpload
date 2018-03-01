package com.changbei.modules.storage.dao;

import org.springframework.data.repository.CrudRepository;

import com.changbei.modules.storage.entity.File;

public interface FileDao  extends  CrudRepository<File, Long> {

}
