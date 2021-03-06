package com.sucaiji.cjpan.web;


import com.sucaiji.cjpan.config.Property;
import com.sucaiji.cjpan.entity.Index;
import com.sucaiji.cjpan.service.IndexService;
import com.sucaiji.cjpan.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sucaiji.cjpan.config.Property.PARENT_UUID;
import static com.sucaiji.cjpan.config.Property.ROOT;
import static com.sucaiji.cjpan.config.Property.TYPE;

@RestController
@RequestMapping("/api")
public class ApiController {
    final static Logger logger = LoggerFactory.getLogger(ApiController.class);
    @Autowired
    private IndexService indexService;
    @Autowired
    private UserService userService;



    /**
     * 相同文件已经存在，返回值告诉客户端秒传
     */
    public static final int MD5_EXIST=1;
    /**
     * 不存在相同文件
     */
    public static final int MD5_NOT_EXIST=2;
    /**
     * 分片已经存在
     */
    public static final int SLICE_EXIST=3;
    /**
     * 分片不存在
     */
    public static final int SLICE_NOT_EXIST=4;
    /**
     * 分片上传成功
     */
    public static final int SUCCESS_SLICE_UPLOAD=999;
    /**
     * 所有分片上传成功
     */
    public static final int SUCCESS_ALL_SLICE_UPLOAD=999;


    @RequestMapping("/exit")
    public void exit(){
        Subject subject=SecurityUtils.getSubject();
        subject.logout();
    }

    /**
     *  获取一共有多少条数据
     *  如果type不为空，且parent_uuid不为空，则查询parent_uuid文件夹下的type类型文件
     *  如果type为空，进入parentUuid目录下获取文件总条数
     *  如果parentUuid和type都为空，获取root目录下文件总条数
     * @param parentUuid
     * @param type
     * @return
     */
    @RequestMapping("/total")
    public Integer total(@RequestParam(value = "parent_uuid",required = false)String parentUuid,
                            @RequestParam(value="type",required = false)String type){

        return indexService.getTotal(parentUuid, type);
    }


    /**
     * 一会添加分页功能
     * @param uuid
     * @param type
     * @param pageSize
     * @param pageNumber
     * @return
     */
    @RequestMapping("/visit")
    public List<Index> visit(@RequestParam(value = "uuid",required = false)String uuid,
                             @RequestParam(value = "type",required = false)String type,
                             @RequestParam(value = "page_size",required = false)Integer pageSize,
                             @RequestParam(value = "page_number",required = false)Integer pageNumber){//带参数uuid就访问那个文件夹 不带的话就主页
        if(uuid==null){
            uuid= ROOT;
        }
        Map<String,Object> map=new HashMap<>();
        map.put(PARENT_UUID,uuid);
        if(type!=null) {
            map.put(TYPE, type);
        }
        List<Index> list=indexService.getIndexList(0,map);
        return list;
        //total pageSize pageNumber
    }



    @RequestMapping("/mkdir")
    public String createDir(@RequestParam("name")String name,
                            @RequestParam(value = "parent_uuid",required = false)String parentUuid){

        if(parentUuid==null){
            parentUuid=ROOT;
        }
        indexService.createDir(name, parentUuid);

        return "success";
    }

    /**
     * 如果已经有该md5值对应的文件了，则告诉客户端已经存在，同时数据库里面添加文件记录
     * @param md5
     * @return
     */
    @RequestMapping(value = "/is_upload")
    public Map<String,Object> isUpload(@RequestParam("md5")String md5,
                                       @RequestParam(value = "parent_uuid",required = false)String parentUuid,
                                       @RequestParam(value = "name")String name){
        if(parentUuid==null){
            parentUuid=ROOT;
        }
        if(indexService.md5Exist(md5)){
            logger.debug("该文件已经存在，进入秒传分支");
            indexService.saveByMd5(md5,parentUuid,name);
            logger.debug("保存一个md5[{}],parentUuid[{}],name[{}]的文件",md5,parentUuid,name);
            Map<String,Object> map=new HashMap<>();
            map.put("flag",MD5_EXIST);
            logger.debug("将信息返回到客户端[{}]",map);
            return map;
        }
        Map<String,Object> map=new HashMap<>();
        map.put("flag",MD5_NOT_EXIST);
        return map;
    }


    @RequestMapping(value = "/upload")
    public Map<String,Object> upload(HttpServletRequest request,
                      @RequestParam(value = "file",required = false)MultipartFile multipartFile,
                      @RequestParam("action")String action,
                      @RequestParam("md5")String md5,//分片的md5
                      @RequestParam("filemd5")String fileMd5,//文件的md5
                      @RequestParam("name")String name,//文件名称
                      @RequestParam(value = "parent_uuid",required = false)String parentUuid,//父uuid，不带此参数的话代表
                      @RequestParam(value = "index")Integer index,//文件第几片
                      @RequestParam(value = "total")Integer total,//总片数
                      @RequestParam(value = "finish",required = false)Boolean finish //是否完成
                        ){
        if(parentUuid==null){
            parentUuid=ROOT;
        }

        if(action.equals("check")){
            Map<String,Object> map=new HashMap<>();
            map.put("flag",SLICE_NOT_EXIST);
            return map;
        }
        if(action.equals("upload")){
            indexService.saveTemp(multipartFile,fileMd5,md5,index);
            //判断传过来的包finish参数是不是true 如果是的话代表是最后一个包，这时开始执行合并校验操作
            if(finish){
                boolean success=indexService.saveFile(parentUuid, fileMd5,name,total);
                if(success) {
                    Map<String,Object> map=new HashMap<>();
                    map.put("flag",123123123);
                    return map;
                }else {
                    Map<String,Object> map=new HashMap<>();
                    map.put("flag",123188883);
                    return map;
                }
            }
            return new HashMap();
        }
        Map<String,Object> map=new HashMap<>();
        map.put("error","mmp你传错值了");
        return map;


    }

    /**
     * 根据传入的uuid，删除该文件或者文件夹，如果是文件夹的话，则删除文件夹下所有文件
     * 注意，删除决定的确定应该在客户端上完成，这里只负责完成删除
     * @param uuid
     * @return
     */
    @RequestMapping("/delete")
    public String delete(@RequestParam("uuid")String uuid){
        indexService.deleteByUuid(uuid);

        return "删完了";
    }
    @RequestMapping("/thumbnail")
    public void thumbnail(@RequestParam("uuid")String uuid,HttpServletResponse response){
        String md5=indexService.getMd5ByUuid(uuid);
        if(md5==null){
            return;
        }
        response.setContentType("image/jpeg");
        response.addHeader("Content-Disposition","attachment;filename="+md5+".jpg");
        File file=indexService.getThumbnailByMd5(md5);
        if(file==null){
            return;
        }
        try {
            OutputStream os=response.getOutputStream();
            indexService.writeInOutputStream(file,os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/image")
    public void image(@RequestParam("uuid")String uuid,
                      HttpServletRequest request,HttpServletResponse response){
        Index index=indexService.getIndexByUuid(uuid);

        response.setContentType("image/jpeg");
        //测试用，测试完删掉
        /*response.setContentType("image/jpeg");//+index.getSuffix());
        try {
            response.addHeader("Content-Disposition","attachment;filename="+ URLEncoder.encode(index.getName(), "UTF-8"));//url这个是将文件名转码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }*/
        try {
            OutputStream os=response.getOutputStream();
            indexService.writeInOutputStream(index,os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/login")
    public String login(HttpSession session,
                        @RequestParam("email")String email,
                        @RequestParam("password")String password){
        Subject subject=SecurityUtils.getSubject();
        UsernamePasswordToken token=new UsernamePasswordToken(email,password);
        try {
            subject.login(token);
        } catch (Exception e){
            return "failure";
        }
        return "success";
    }

    @RequestMapping("/init_regist")
    public String initRegister(
                            HttpServletRequest request,
                            HttpServletResponse response,
                            @RequestParam("email")String email,
                            @RequestParam("password")String password,
                            @RequestParam("name")String name){
        if(!userService.isEmpty()){
            try {
                response.sendRedirect("/index");
            } catch (IOException e) {
                e.printStackTrace();
            }
            //如果user表不是空的则什么也不做
            return "321321";
        }
        //魔法值admin后期改
        userService.regist(email,password,name,"admin");
        try {
            response.sendRedirect("/index");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "123123";
    }

    @RequestMapping("/video")
    public void video(@RequestParam("uuid")String uuid,
                      HttpServletRequest request,HttpServletResponse response){
        Index index=indexService.getIndexByUuid(uuid);
        response.setContentType("video/mp4");//+index.getSuffix());


        try {
            response.addHeader("Content-Disposition","attachment;filename="+ URLEncoder.encode(index.getName(), "UTF-8"));//url这个是将文件名转码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setHeader("Accept-Ranges","bytes");


        String rangeStr=request.getHeader("range");
        if(rangeStr!=null){
            IndexService.Range range=indexService.getRange(rangeStr,index.getSize());
            String total=String.valueOf(range.total);
            String length=String.valueOf(range.length);
            String start=String.valueOf(range.start);
            String end=String.valueOf(range.end);

            response.setHeader("Content-Range","bytes "+start+"-"+end+"/"+total);
            response.setHeader("Content-Length",length);

            System.out.println("range不为空");
            System.out.println("rangeStr="+rangeStr);
            System.out.println("start"+range.start);
            System.out.println("end"+range.end);
            System.out.println("length"+range.length);
            System.out.println("total"+range.total);
            response.setStatus(206);
            try {
                OutputStream os=response.getOutputStream();
                indexService.writeInOutputStream(index,os,range);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            Long end=index.getSize()-1L;
            IndexService.Range range=new IndexService.Range(0L,end,index.getSize());
            String length=String.valueOf(index.getSize());
            System.out.println("range为空!");
            response.setStatus(200);
            response.setHeader("Content-Range", "bytes 0-"+end+"/"+index.getSize());
            response.setHeader("Content-Length", length);


            try {
                OutputStream os=response.getOutputStream();
                indexService.writeInOutputStream(index,os,range);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(response.getHeader("Content-Range"));
        System.out.println(response.getHeader("Content-Length"));




    }



    @RequestMapping("/download")
    public void download(HttpServletRequest request, HttpServletResponse response){
        String uuid=request.getParameter("uuid");
        if(uuid==null){
            return;
        }
        Index index=indexService.getIndexByUuid(uuid);
        String fileName=index.getName();
        Long fileLength=index.getSize();
        response.setContentType("application/force-download");
        try {
            response.addHeader("Content-Disposition","attachment;filename="+ URLEncoder.encode(fileName, "UTF-8"));//url这个是将文件名转码
            response.setHeader("Content-Length", String.valueOf(fileLength));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            //文件丢失时，这里会下载0kb的空文件
            //下次将writeInoutputStream函数修改一下
            OutputStream os=response.getOutputStream();
            indexService.writeInOutputStream(uuid,os);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}









