package com.changbei.modules.storage.web;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Field.Index;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.changbei.common.config.Global;
import com.changbei.common.mapper.JsonMapper;
import com.changbei.common.service.ServiceException;
import com.changbei.common.web.BaseController;
import com.changbei.modules.storage.FileUtil;
import com.changbei.modules.storage.entity.FileInfo;
import com.changbei.modules.storage.service.FileService;
/**
 * 文件操作
 * TODO 参数接收有乱码未解决 
 * @author lgq
 *
 */
@Controller
@RequestMapping(value = Global.ADMIN_PATH + "/storage")
public class FileController extends BaseController {

	@Autowired
	private FileService fileService;

	/**
	 * 文件上传测试页面
	 * 
	 * @param path  文件路径
	 * @return
	 */
	@RequestMapping(value = "page")
	public String page(HttpServletRequest request) throws ServiceException {
		return "modules/storage/Index";
	}
	
	/**
	 * 文件列表
	 * 
	 * @param path  文件路径
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "list")
	public String list(HttpServletRequest request) throws ServiceException {
		try {
			String path =getParameter(request, "path");
			return JsonMapper.getInstance().toJson(fileService.list(path));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(e.getMessage());
		}
	}
	
	/**
	 * 文件上传（支持大文件）
	 * 
	 * @param path 文件路径
	 * @param relevanceId 关联id
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "upload")
	public String upload(HttpServletRequest request,FileInfo info) throws ServiceException {
		try {
				initFileInfo(info,request); //初始化文件上传信息
			  if (info.getChunks() != null && info.getChunk() != null) { //大文件分片上传
				 	fileService.savePartFile(info);	// 将文件分块保存到临时文件夹里，便于之后的合并文件
	                fileService.bigUploaded(info); //完整的上传
	    			return "success！";
			  }else { //普通小文件上传
				  try {			
						fileService.upload(FileUtil.getMultipartFile(request), info.getPath(), info.getRelevanceId());
						return "上传成功！";
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
	 * @param info
	 * @param request
	 */
	@ResponseBody
	@RequestMapping(value = "md5/exist")
	public String md5Exist(String fileMd5, String fileName, String fileID){
		try {
            return String.valueOf(fileService.isMd5Exist(fileMd5));
        } catch (Exception e) {
            e.printStackTrace();
            return "false";
        }
	}
	
	/**
	 * 初始化上传文件信息
	 * @param info
	 * @param request
	 */
	private void initFileInfo(FileInfo info,HttpServletRequest request){
		String path =getParameter(request, "path");
		String value = request.getParameter("relevanceId");
		Long relevanceId=null;
		if(null!=value){
			relevanceId = Long.parseLong(value);
		}
		int  ext_index = info.getName().lastIndexOf(".");
		String ext =null;
		if(ext_index!=-1){			
			info.getName().substring(ext_index);//后缀
		}else {			
			ext = null==ext?"":ext;
		}
        info.setExt(ext);
        if(null!=info.getChunk()){
        	int index = Integer.parseInt(info.getChunk()); //文件分片序号
    	 	String partName = String.valueOf(index) + ext; //分片文件保存名
    	 	info.setPartName(partName);
        }
	 	info.setRelevanceId(relevanceId);
	 	info.setPath(path);
	 	
	}


	/**
	 * 文件下载（支持大文件下载）
	 * 
	 * @param path 文件路径
	 * @param isFolder 是否为文件夹，默认为文件(下载文件夹设置为true)
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "download")
	public void downLoad(HttpServletResponse response,
			HttpServletRequest request,boolean isFolder) throws ServiceException {
		try {
			String path =getParameter(request,"path");
			File file = fileService.download(path,isFolder);
			if(file.exists()){
				writeOutFile(response, file);
				if(isFolder){
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
	 * @param dir 文件夹名
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "mkdir")
	public String mkdir(HttpServletRequest request) throws ServiceException {
		try {
			String path =getParameter(request, "path");
			String dir =getParameter(request, "dir");
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
	 * @param path 文件路径
	 * @param newName 新的文件名           
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "rename")
	public String rename(HttpServletRequest request) throws ServiceException {
		try {
			String path =getParameter(request, "path");
			String newName =getParameter(request,"newName");
			fileService.rename(path, newName);
			return "重命名成功!";

		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 * 移动文件至文件夹
	 * @param path 文件路径            
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "move")
	public String move(HttpServletRequest request) throws ServiceException {
		try {
			String fromPath =getParameter(request,"fromPath");
			String toPath = getParameter(request,"toPath");
			fileService.move(fromPath, toPath);
			return "移动文件成功！";
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceException(e.getMessage());
		}
	}

	/**
	 * 删除文件
	 * @param paths 多个文件路径
	 * @return
	 * @throws ServiceException
	 */
	@ResponseBody
	@RequestMapping(value = "delfile")
	public String delFile(HttpServletRequest request) throws ServiceException {
		try {
			String[] paths = request.getParameterValues("paths");
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
	 * @param path 文件路径
	 * @param response
	 * @throws ServiceException
	 */
	@ResponseBody
	@RequestMapping(value = "thumbnails")
	public void thumbnails(HttpServletRequest request,
			HttpServletResponse response) throws ServiceException {
		String path =getParameter(request,"path");
		try {
			writeImage(response, fileService.thumbnails(path));
		} catch (IOException e) {
			e.printStackTrace();
			throw new ServiceException("获取缩略图错误！");
		}
	}
	/**
	 * 通过name获取参数
	 * @param request
	 * @return
	 * @throws ServiceException
	 */
	private String getParameter(HttpServletRequest request,String name)throws ServiceException{
		String parameter= request.getParameter(name);
		if (StringUtils.isNotBlank(parameter)) {
			try {
				if(!isContainChinese(parameter)){
					return new String(parameter.getBytes("ISO-8859-1"), "UTF-8");	
				}
				return parameter;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				throw new ServiceException(e.getMessage());
			}
		} else {
			throw new ServiceException(name+"不能为空!");
		}
	}
	
	public static boolean isContainChinese(String str) {
		 Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
		 Matcher m = p.matcher(str);
		 if (m.find()) {
		  return true;
		 }
		 return false;
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
	 * @param bytes
	 * @param fileName
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

}
