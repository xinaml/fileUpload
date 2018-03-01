package com.xinaml.fileupload.service;

import com.changbei.modules.storage.vo.FileVO;
import com.xinaml.fileupload.FileUtil;
import com.xinaml.fileupload.common.PathCommon;
import com.xinaml.fileupload.entity.FileInfo;
import com.xinaml.fileupload.exception.ServiceException;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;


@Service
public class FileService {

    private static final String[] IMAGE_SUFFIX = new String[]{"jpg", "png",
            "jpeg", "bmp", "gif"};
    private static Map<String, Integer> uploadInfoList = new HashMap<String, Integer>();

    /**
     * 文件信息列表
     *
     * @param path 路径
     * @return
     * @throws ServiceException
     */
    public List<FileVO> list(String path) throws ServiceException {
        String realPath = getRealPath(path);
        java.io.File dir = new java.io.File(realPath);
        java.io.File[] files = dir.listFiles();
        return getFileVO(files);
    }


    /**
     * 上传文件
     *
     * @param mFiles      上传文件列表
     * @param path        上传路径
     * @param relevanceId 关联id
     * @return 返回上传文件信息
     * @throws ServiceException
     */
    public List<FileVO> upload(List<MultipartFile> mFiles, String path, Long relevanceId)
            throws ServiceException {
        String realPath = getRealPath(path);
        File[] files = new File[mFiles.size()];
        try {
            int index = 0;
            for (MultipartFile mfile : mFiles) {
                File file = new File(realPath + PathCommon.SEPARATOR + mfile.getOriginalFilename());
                mfile.transferTo(file);
                //save db
                if (null != relevanceId) {
                    saveFileToDb(file, null);
                }

                files[index++] = file;
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw new ServiceException("文件上传异常!");
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceException("文件上传异常!");
        }
        return getFileVO(files);
    }

    /**
     * 创建文件夹
     *
     * @param path 路径
     * @param dir  创建文件夹名
     * @throws ServiceException
     */
    public void mkDir(String path, String dir) throws ServiceException {
        String realPath = getRealPath(path);
        java.io.File file = new java.io.File(realPath + PathCommon.SEPARATOR + dir);
        if (!file.exists()) {
            file.mkdirs();
        } else {
            throw new ServiceException("该文件目录已存在！");
        }
    }

    /**
     * 删除文件
     *
     * @param paths
     * @throws ServiceException
     */
    public void delFile(String[] paths) throws ServiceException {
        for (String path : paths) {
            String savePath = getRealPath(path);
            java.io.File file = new java.io.File(savePath);
            if (file.exists()) {
                if (file.isFile()) {
                    file.delete();
                } else {// 删除目录及目录下的所有文件
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new ServiceException(e.getMessage());
                    }
                }

            } else {
                throw new ServiceException("该文件目录不存在！");
            }
        }

    }

    /**
     * 重命名
     *
     * @param path    路径
     * @param newName 名字
     * @throws ServiceException
     */
    public void rename(String path, String newName) throws ServiceException {
        String realPath = getRealPath(path);
        String oldName = StringUtils.substringAfterLast(realPath, "/");
        String savePath = StringUtils.substringBeforeLast(realPath, "/");
        if (!oldName.equals(newName)) {// 新的文件名和以前文件名不同时,才有必要进行重命名
            java.io.File oldFile = new java.io.File(realPath);
            java.io.File newFile = new java.io.File(savePath + "/" + newName);
            if (!oldFile.exists()) {// 重命名文件不存在
                throw new ServiceException("重命名文件不存在！");
            }
            if (newFile.exists())
                throw new ServiceException(newName + "已经存在！");
            else {
                oldFile.renameTo(newFile);
                String oldPath = getDbFilePath(oldFile);
                String newPath = getDbFilePath(newFile);

            }
        } else {
            throw new ServiceException("新文件名和旧文件名相同!");
        }
    }

    /**
     * 下载
     *
     * @param path 文件路径
     * @return
     * @throws ServiceException
     */
    public File download(String path, boolean isFolder) throws ServiceException {
        File file = null;
        if (isFolder) {
            String savePath = getRealPath(path);
            try {
                String zipSavePath = StringUtils.substringBeforeLast(savePath, "/") + PathCommon.SEPARATOR + StringUtils.substringAfterLast(path, "/") + ".zip";
                file = new File(zipSavePath);
                FileOutputStream outputStream = null;
                outputStream = new FileOutputStream(file);
                FileUtil.toZip(savePath, outputStream, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        } else {
            String realPath = getRealPath(path);
            file = new File(realPath);
        }
        return file;

    }

    /**
     * 获取缩略图
     *
     * @param path 文件路径
     * @return
     * @throws ServiceException
     */
    public byte[] thumbnails(String path) throws ServiceException {
        String realPath = getRealPath(path);
        String suffix = StringUtils.substringAfterLast(path, ".");
        boolean exist = false;
        for (String sx : IMAGE_SUFFIX) {
            if (sx.equalsIgnoreCase(suffix)) {
                exist = true;
            }
        }
        if (exist) {
            try {
                Thumbnails.Builder<java.io.File> fileBuilder = Thumbnails
                        .of(realPath).forceSize(200, 160).outputQuality(0.35f)
                        .outputFormat(suffix);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                fileBuilder.toOutputStream(os);
                return os.toByteArray();
            } catch (IOException e) {
                throw new ServiceException("缩略图获取错误");
            }
        } else {
            throw new ServiceException("不支持该文件类型缩略图");
        }

    }

    /**
     * 文件是否存在
     *
     * @param path
     * @return
     * @throws ServiceException
     */
    public Boolean existsFile(String path) throws ServiceException {
        String realPath = getRealPath(path);
        return new java.io.File(realPath).exists();
    }

    /**
     * 文件移动
     *
     * @param fromPath
     * @param toPath
     * @return
     * @throws ServiceException
     */
    public Boolean move(String fromPath, String toPath) throws ServiceException {
        String from = getRealPath(fromPath);
        String to = getRealPath(toPath);
        java.io.File fromFile = new java.io.File(from);
        java.io.File toFile = new java.io.File(to);
        if (toFile.isFile()) {
            throw new ServiceException("不允许移动文件或文件夹到文件下！");
        } else {
            try {
                if (fromFile.isFile()) {
                    FileUtils.moveFileToDirectory(
                            fromFile, toFile, true);
                } else {
                    FileUtils.moveDirectoryToDirectory(
                            fromFile, toFile, true);
                }

            } catch (IOException e) {
                throw new ServiceException(e.getMessage());
            }
        }
        return true;
    }


    /**
     * /**
     * 保存分片文件
     *
     * @param info
     * @return
     * @throws Exception
     */
    public boolean savePartFile(FileInfo info)
            throws Exception {
        String savePath = getRealPath(info.getPath()) + PathCommon.SEPARATOR + info.getGuid();//以文件名创建一个临时保存目录
        File uploadFile = new File(savePath + PathCommon.SEPARATOR + info.getPartName());

        File fileDirectory = new File(savePath);
        synchronized (fileDirectory) { //判断文件夹是否存在，不存在就创建一个
            if (!fileDirectory.exists()) {
                fileDirectory.mkdir();
            }
        }
        info.getFile().transferTo(uploadFile);
        return uploadFile.exists();
    }


    /**
     * 完整上传
     */
    public void bigUploaded(FileInfo f) throws ServiceException {
        synchronized (uploadInfoList) {
            if (f.getMd5value() != null) {
                Integer size = uploadInfoList.get(f.getMd5value());
                size = size != null ? size : 0;
                if (null != size) {
                    uploadInfoList.put(f.getMd5value(), size + 1);
                }
            }
        }
        boolean allUploaded = isAllUploaded(f.getMd5value(), f.getChunks());
        if (allUploaded) { //已上传完整
            mergeFile(f);//合并文件
            File file = new File(getRealPath(f.getPath()) + PathCommon.SEPARATOR + f.getName());
            if (file.exists()) {
                saveFileToDb(file, f.getMd5value());
            }
        }
    }

    /**
     * 查询md5是否存在，实现秒传
     *
     * @param md5
     * @return
     */
    public boolean isMd5Exist(String md5) {
//    	List<com.xinaml.fileupload.entity.File> list = findBySql("select md5 from tb_storage_file where md5='"+md5+"'", com.xinaml.fileupload.entity.File.class, "md5");
//    	return (null!=list && list.size()>0);
        return false;
    }

    /**
     * 是否上传完成
     *
     * @param md5
     * @param chunks
     * @return
     */
    public boolean isAllUploaded(String md5, String chunks) {
        Integer size = uploadInfoList.get(md5); //上传完成的部分
        size = size != null ? size : 0;
        boolean bool = (size == Integer.parseInt(chunks)); //上传的部分跟总的分片数一致则代表上传完成
        if (bool) {
            synchronized (uploadInfoList) {
                //删除缓存
                for (Entry<String, Integer> entry : uploadInfoList.entrySet()) {
                    if (entry.getKey().equals(md5)) {
                        uploadInfoList.remove(entry.getKey());
                    }
                }
            }
        }
        return bool;
    }

    /**
     * 合并文件
     *
     * @param info
     * @throws ServiceException
     */
    private void mergeFile(FileInfo info) throws ServiceException {
        /* 合并输入流 */
        String uploadPath = getRealPath(info.getPath());
        String mergePath = uploadPath + PathCommon.SEPARATOR + info.getGuid() + PathCommon.SEPARATOR;

        SequenceInputStream s;
        int chunksNumber = Integer.parseInt(info.getChunks());
        try {
            InputStream s1 = new FileInputStream(mergePath + 0 + info.getExt());
            InputStream s2 = new FileInputStream(mergePath + 1 + info.getExt());
            s = new SequenceInputStream(s1, s2);
            for (int i = 2; i < chunksNumber; i++) {
                InputStream s3 = new FileInputStream(mergePath + i + info.getExt());
                s = new SequenceInputStream(s, s3);
            }
            FileUtil.saveStreamToFile(s, uploadPath + PathCommon.SEPARATOR + info.getName()); //合并文件
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("合并文件失败");
        } finally {
            //删除缓存
            for (Entry<String, Integer> entry : uploadInfoList.entrySet()) {
                if (entry.getKey().equals(info.getMd5value())) {
                    uploadInfoList.remove(entry.getKey());
                }
            }
            try {
                FileUtils.deleteDirectory(new File(mergePath));// 删除保存分块文件的文件夹
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    /**
     * 获取真实路径
     *
     * @param path 路径
     * @return
     */
    private String getRealPath(String path) throws ServiceException {

        return PathCommon.ROOT_PATH + path;

    }

    /**
     * 通过文件获取保存数据库对应的数据
     *
     * @param file
     * @return
     */
    private String getDbFilePath(java.io.File file) {
        return PathCommon.SEPARATOR + StringUtils.substringAfterLast(file.getPath(),
                PathCommon.ROOT_PATH + PathCommon.SEPARATOR);

    }

    /**
     * 通过文件列表获取文件的详细信息
     *
     * @param files
     * @return
     */
    private List<FileVO> getFileVO(java.io.File[] files)
            throws ServiceException {
        String rootPath = getRealPath("/");
        if (null != files) {
            List<FileVO> fileVOS = new ArrayList<FileVO>(files.length);
            for (int i = 0; i < files.length; i++) {
                FileVO fileVO = new FileVO();
                java.io.File file = files[i];
                fileVO.setFileType(FileUtil.getFileType(file));
                if (file.isFile()) {
                    fileVO.setSize(FileUtil.getFileSize(file));
                    fileVO.setLength(file.length());
                }

                fileVO.setPath(StringUtils.substringAfter(file.getPath(),
                        rootPath));
                fileVO.setDir(file.isDirectory());
                fileVO.setName(file.getName());
                //fileVO.setCreateTime(file.getName());
                fileVO.setModifyTime(new Date(file.lastModified()).toString());
                fileVOS.add(fileVO);
            }

            return fileVOS;
        }
        return new ArrayList<FileVO>(0);
    }

    /**
     * 保存文件信息好数据库
     *
     * @param file
     */
    private void saveFileToDb(File file, String md5) {
        com.xinaml.fileupload.entity.File dbFile = new com.xinaml.fileupload.entity.File();
        dbFile.setFileType(FileUtil.getFileType(file));
        dbFile.setPath(getDbFilePath(file));
        dbFile.setName(file.getName());
        dbFile.setUserId(null);
        dbFile.setSize(FileUtil.getFileSize(file));
        dbFile.setCreateDate(new Date());
        dbFile.setMd5(md5);
        dbFile.setDown(true);
        //fileDao.save(dbFile);
    }


}