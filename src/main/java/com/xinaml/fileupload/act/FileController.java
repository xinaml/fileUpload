package com.xinaml.fileupload.act;

import com.alibaba.fastjson.JSON;
import com.xinaml.fileupload.FileUtil;
import com.xinaml.fileupload.entity.FileInfo;
import com.xinaml.fileupload.exception.ServiceException;
import com.xinaml.fileupload.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * 文件操作
 *
 * @author lgq
 */
@Controller
@RequestMapping(value = {"/storage"})
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 文件上传测试页面
     *
     * @return
     */
    @RequestMapping(value = "page")
    public String page() throws ServiceException {
        return "index";
    }

    /**
     * 文件列表
     *
     * @param path 文件路径
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "list")
    public String list(String path) throws ServiceException {
        try {
            return JSON.toJSONString(fileService.list(path));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 文件上传（支持大文件）
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "upload")
    public String upload(HttpServletRequest request, FileInfo info) throws ServiceException {
        try {
            initFileInfo(info); //初始化文件上传信息
            if (info.getChunks() != null && info.getChunk() != null) { //大文件分片上传
                fileService.savePartFile(info);    // 将文件分块保存到临时文件夹里，便于之后的合并文件
                fileService.bigUploaded(info); //完整的上传
                return "success！";
            } else { //普通小文件上传
                try {
                    fileService.upload(FileUtil.getMultipartFile(request), info.getPath(), info.getRelevanceId());
                    return "上传传传成功！";
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ServiceException(e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }

    }

    /**
     * 检测MD5是否存在，实现秒传
     *
     * @param fileID
     * @param fileName
     * @param fileMd5
     */
    @ResponseBody
    @RequestMapping(value = "md5/exist")
    public String md5Exist(String fileMd5, String fileName, String fileID) {
        try {
            return String.valueOf(fileService.isMd5Exist(fileMd5));
        } catch (Exception e) {
            e.printStackTrace();
            return "false";
        }
    }



    /**
     * 文件下载（支持大文件下载）
     *
     * @param path     文件路径
     * @param isFolder 是否为文件夹，默认为文件(下载文件夹设置为true)
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "download")
    public void downLoad(HttpServletResponse response,
                         String path, boolean isFolder) throws ServiceException {
        try {
            File file = fileService.download(path, isFolder);
            if (file.exists()) {
                writeOutFile(response, file);
                if (isFolder) {
                    file.delete(); //删除压缩文件
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("下载异常!");
        }
    }

    /**
     * 创建文件夹
     *
     * @param path 文件路径
     * @param dir  文件夹名
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "mkdir")
    public String mkdir(String path, String dir) throws ServiceException {
        try {
            fileService.mkDir(path, dir);
            return "创建文件夹成功!";

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 重命名文件或文件夹
     *
     * @param path    文件路径
     * @param newName 新的文件名
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "rename")
    public String rename(String path, String newName) throws ServiceException {
        try {
            fileService.rename(path, newName);
            return "重命名成功!";

        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 移动文件至文件夹
     *
     * @param fromPath 文件路径
     * @param toPath   文件路径
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "move")
    public String move(String fromPath, String toPath) throws ServiceException {
        try {
            fileService.move(fromPath, toPath);
            return "移动文件成功！";
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param paths 多个文件路径
     * @return
     * @throws ServiceException
     */
    @ResponseBody
    @RequestMapping(value = "delfile")
    public String delFile(String[] paths) throws ServiceException {
        try {
            if (null != paths) {
                int index = 0;
                for (String path : paths) {
                    paths[index++] = new String(path.getBytes("ISO-8859-1"),
                            "UTF-8");
                }
                fileService.delFile(paths);
                return "删除文件成功!";
            } else {
                throw new ServiceException("paths不能为空");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 获取缩略图
     *
     * @param path     文件路径
     * @param response
     * @throws ServiceException
     */
    @ResponseBody
    @RequestMapping(value = "thumbnails")
    public void thumbnails(HttpServletResponse response, String path) throws ServiceException {
        try {
            writeImage(response, fileService.thumbnails(path));
        } catch (IOException e) {
            e.printStackTrace();
            throw new ServiceException("获取缩略图错误！");
        }
    }


    /**
     * 输出图片
     *
     * @param response
     * @param bytes
     * @throws IOException
     */
    private void writeImage(HttpServletResponse response, byte[] bytes)
            throws IOException {
        response.reset();
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/jpeg");
        ByteArrayInputStream in = new ByteArrayInputStream(bytes); // 将b作为输入流；
        BufferedImage image = ImageIO.read(in);
        ImageIO.write(image, "jpg", response.getOutputStream());

    }

    /**
     * 输出大文件
     *
     * @param response
     * @param file
     * @throws IOException
     */
    private void writeOutFile(HttpServletResponse response, File file
    ) throws IOException {
        try {
            if (file.exists()) {
                String dfileName = file.getName();
                InputStream fis = new BufferedInputStream(new FileInputStream(
                        file));
                response.reset();
                response.setContentType("application/x-download");
                response.addHeader(
                        "Content-Disposition",
                        "attachment;filename="
                                + new String(dfileName.getBytes(), "iso-8859-1"));
                response.addHeader("Content-Length", "" + file.length());
                OutputStream toClient = new BufferedOutputStream(
                        response.getOutputStream());
                response.setContentType("application/octet-stream");
                byte[] buffer = new byte[1024 * 1024 * 4];
                int i = -1;
                while ((i = fis.read(buffer)) != -1) {
                    toClient.write(buffer, 0, i);

                }
                fis.close();
                toClient.flush();
                toClient.close();

            } else {
                PrintWriter out = response.getWriter();
                out.print("<script>");
                out.print("alert(\"not find the file\")");
                out.print("</script>");
            }
        } catch (IOException ex) {
            PrintWriter out = response.getWriter();
            out.print("<script>");
            out.print("alert(\"not find the file\")");
            out.print("</script>");
        }
    }


    /**
     * 初始化上传文件信息
     *
     * @param info
     */
    private void initFileInfo(FileInfo info) {

        int ext_index = info.getName().lastIndexOf(".");
        String ext = null;
        if (ext_index != -1) {
            info.getName().substring(ext_index);//后缀
        } else {
            ext = null == ext ? "" : ext;
        }
        info.setExt(ext);
        if (null != info.getChunk()) {
            int index = Integer.parseInt(info.getChunk()); //文件分片序号
            String partName = String.valueOf(index) + ext; //分片文件保存名
            info.setPartName(partName);
        }


    }

}
