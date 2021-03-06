package com.sucaiji.cjpan.web;

import com.sucaiji.cjpan.config.Property;
import com.sucaiji.cjpan.entity.Index;
import com.sucaiji.cjpan.service.IndexService;
import com.sucaiji.cjpan.service.UserService;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sucaiji.cjpan.config.Property.*;


@Controller
public class MainController {
    final static Logger logger= LoggerFactory.getLogger(MainController.class);
    @Autowired
    private IndexService indexService;
    @Autowired
    private UserService userService;

    @RequestMapping("/login")
    public String login() {
        if (userService.isEmpty()) {
            return "init";
        }
        return "login";
    }

    @RequestMapping("/init")
    public String init() {
        return "init";
    }

    @RequestMapping("/upload")
    public String upload(Response response, Request request, Model model) {
        return "upload";
    }

    @RequestMapping(value = {"/", "index", "/index"})
    public String index(@RequestParam(value = "parent_uuid", required = false) String parentUuid,
                        @RequestParam(value = "pg", required = false) Integer pageNumber,
                        Model model) {
        if (parentUuid == null) {
            parentUuid = ROOT;
        }
        logger.debug("用户请求的parentUuid为[{}]",parentUuid);
        if (pageNumber == null) {
            pageNumber = 1;
        }
        logger.debug("用户请求的pageNumber为[{}]",pageNumber+"");
        List<Index> list;
        list = indexService.getIndexList(pageNumber, parentUuid);
        model.addAttribute("indexList", list);

        int total = indexService.getTotal(parentUuid,null);
        int pageAmount = (int) Math.ceil((double) total/(double)indexService.DEFAULT_PAGE_SIZE);
        model.addAttribute("currentPage",pageNumber);
        model.addAttribute("pageAmount",pageAmount);


        if (parentUuid == null) {
            model.addAttribute("parentIndex", null);
            return "index";
        }
        Index index = indexService.getIndexByUuid(parentUuid);
        model.addAttribute("parentIndex", index);
        return "demo";
    }

    @RequestMapping("/file/{str}")
    public String type(@PathVariable("str") String str,
                       @RequestParam(value = "pg" , required = false) Integer pageNumber,
                       Model model) {
        if (pageNumber == null) {
            pageNumber = 1;
        }
        String type;
        switch (str) {
            case "gallery":
                type = IMAGE;
                break;
            case "documents":
                type = DOCUMENT;
                break;
            case "videos":
                type = VIDEO;
                break;
            case "musics":
                type = MUSIC;
                break;
            default:
                type = OTHER;
        }

        Map<String, Object> map = new HashMap<>();
        map.put(TYPE, type);

        List<Index> list;
        list = indexService.getIndexList(pageNumber, map);
        model.addAttribute("indexList", list);

        int total = indexService.getTotal(null,type);
        int pageAmount = (int) Math.ceil((double) total/(double)indexService.DEFAULT_PAGE_SIZE);
        System.out.println(total+"+"+pageAmount);
        model.addAttribute("currentPage",pageNumber);
        model.addAttribute("pageAmount",pageAmount);

        return "demo2";

    }


    @RequestMapping("/download")
    public String download() {
        return "download";
    }


    @RequestMapping("/video/{uuid}")
    public String video(@PathVariable("uuid") String uuid,
                        Model model) {
        Index index = indexService.getIndexByUuid(uuid);
        model.addAttribute("index", index);
        return "video";
    }




    @RequestMapping("/test")
    public String test(HttpServletRequest request, Model model) {
        String parentUuid = request.getParameter("parent_uuid");
        List<Index> list;
        list = indexService.getIndexList(parentUuid);
        model.addAttribute("indexList", list);

        return "test";
    }


}
