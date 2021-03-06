package com.sucaiji.cjpan.service;

import com.sucaiji.cjpan.entity.Index;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public interface IndexService {
    //默认每页数量
    Integer DEFAULT_PAGE_SIZE=20;

    /**
     * 创建文件夹，如果parentUuid为空，则在根目录创建文件夹
     * @param name
     * @param parentUuid
     */
    void createDir(String name, String parentUuid);

    /**
     * 通过某个文件夹的uuid获取文件夹下所有文件
     *
     * @param parentUuid
     * @return
     */
    List<Index> getIndexList(String parentUuid);

    /**
     * 通过某个文件夹的uuid获取文件夹下的第page页文件
     *
     * @param page
     * @param parentUuid
     * @return
     */
    List<Index> getIndexList(Integer page, String parentUuid);

    /**
     * 通过某个文件夹的uuid获取文件夹下的文件
     * @param page
     * @param map
     * @return
     */
    List<Index> getIndexList(Integer page, Map<String, Object> map);

    /**
     * 通过某个文件夹的uuid获取文件夹下的文件
     * @param page
     * @param pageSize
     * @param map
     * @return
     */
    List<Index> getIndexList(Integer page , Integer pageSize, Map<String, Object> map);

    /**
     * 获取数据条数
     * @param parentUuid
     * @param type
     * @return
     */
    Integer getTotal(String parentUuid, String type);


    /**
     * @return
     */
    Index getIndexByUuid(String uuid);

    /**
     * 通过uuid获取到md5值,如果不存在则返回null
     *
     * @param uuid
     * @return
     */
    String getMd5ByUuid(String uuid);

    /**
     * 根据md5获取到缩略图文件,如果缩略图不存在则返回null
     *
     * @param md5
     * @return
     */
    File getThumbnailByMd5(String md5);

    /**
     * 根据传入的uuid获取到文件，并写入到传入的outputstream里面
     *
     * @param uuid
     * @param os
     */
    void writeInOutputStream(String uuid, OutputStream os) throws IOException;

    /**
     * 根据传入的index获取到文件，并写入到传入的outputstream里面
     *
     * @param index
     * @param os
     */
    void writeInOutputStream(Index index, OutputStream os) throws IOException;

    /**
     * 将传入的file写入到outputStream里面
     *
     * @param file
     * @param os
     * @throws IOException
     */
    void writeInOutputStream(File file, OutputStream os) throws IOException;

    void writeInOutputStream(String uuid, OutputStream os, Range range) throws IOException;

    void writeInOutputStream(Index index, OutputStream os, Range range) throws IOException;

    /**
     * 根据偏移量将数据写入流中
     *
     * @param file
     * @param os
     * @param range 偏移量
     * @throws IOException
     */
    void writeInOutputStream(File file, OutputStream os, Range range) throws IOException;

    Range getRange(String rangeStr, Long fileSize);

    /**
     * 文件合并和保存到数据库,并生成缩略图
     *
     * @param parentUuid
     * @param fileMd5
     * @param name
     * @param total
     * @return
     */
    boolean saveFile(String parentUuid, String fileMd5, String name, int total);

    /**
     * 将接收到的分片包保存在临时文件夹
     *
     * @param multipartFile
     * @param fileMd5
     * @param md5
     * @param index
     */
    void saveTemp(MultipartFile multipartFile, String fileMd5, String md5, Integer index);

    /**
     * 当服务器中有该文件时，通过md5值秒存（在数据库index表中添加一条新纪录）
     *
     * @param md5
     * @param parentUuid
     * @param name
     */
    void saveByMd5(String md5, String parentUuid, String name);

    /**
     * 通过uuid获取文件实际所在位置，如果文件不存在则返回null
     *
     * @param uuid
     * @return
     */
    File getFileByUuid(String uuid);

    /**
     * 判断该md5对应的文件是否存在
     *
     * @param md5
     * @return
     */
    boolean md5Exist(String md5);

    /**
     * 删除某个文件，如果该uuid指向一个文件夹的话，则删除该文件夹下所有文件
     *
     * @param uuid
     */
    void deleteByUuid(String uuid);

    class Range {
        public Long start;
        public Long end;
        public Long length;
        public Long total;

        public Range(Long start, Long end, Long total) {
            this.start = start;
            this.end = end;
            this.length = end - start + 1;
            this.total = total;
        }
    }

}
