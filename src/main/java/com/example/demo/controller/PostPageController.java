package com.example.demo.controller;

import com.example.demo.config.SessionManager;
import com.example.demo.dto.PostDetailDto;
import com.example.demo.entity.User;
import com.example.demo.service.PostService;
import org.springframework.web.bind.annotation.CookieValue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PostPageController {

    private final PostService postService;
    private final SessionManager sessionManager;

    @GetMapping(value = "/post/{id}", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String postDetailPage(@PathVariable Long id,
                                 @CookieValue(value = "sessionId", required = false) String sessionId) {
        try {
            User viewer = sessionManager.getUser(sessionId);
            PostDetailDto postDetail = postService.getPostDetailById(id, viewer);

            String title = postDetail.getTitle() == null ? "帖子详情" : postDetail.getTitle();
            String description = buildDescription(postDetail.getContent());

            // 构建包含话题名称的JSON
            String postJson = toJsonWithTopics(postDetail).replace("</", "<\\/");

            String authorName = postDetail.getAuthorUsername() == null ? "用户" : postDetail.getAuthorUsername();
            String authorAvatar = postDetail.getAuthorAvatar() == null ? "" : postDetail.getAuthorAvatar();
            String firstLetter = authorName.isEmpty() ? "U" : authorName.substring(0, 1).toUpperCase();

            String authorAvatarHtml = !authorAvatar.isBlank()
                    ? "<img loading=\"lazy\" src=\"" + escapeHtmlAttr(authorAvatar) + "\" alt=\"" + escapeHtmlAttr(authorName) + "\">"
                    : "<span>" + escapeHtml(firstLetter) + "</span>";

            String safeTitle = escapeHtml(title);

            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>");
            sb.append("<html lang=\"zh-CN\">");
            sb.append("<head>");
            sb.append("<meta charset=\"UTF-8\">");
            sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">");
            sb.append("<title>").append(safeTitle).append("</title>");
            sb.append("<meta name=\"description\" content=\"").append(escapeHtmlAttr(description)).append("\">");
            sb.append("<link href=\"https://unpkg.com/@wangeditor/editor@latest/dist/css/style.css\" rel=\"stylesheet\">");
            sb.append("<link href=\"/css/post-detail.css\" rel=\"stylesheet\">");
            sb.append("</head>");
            sb.append("<body>");

            sb.append("<div class=\"page-topbar\">");
            sb.append("<div class=\"topbar-inner\">");
            sb.append("<div class=\"topbar-left\">");
            sb.append("<button class=\"back-btn\" id=\"backBtn\" type=\"button\" aria-label=\"返回\">返回</button>");
            sb.append("<div class=\"topbar-title\" id=\"topbarTitle\">").append(safeTitle).append("</div>");
            sb.append("</div>");
            sb.append("<div class=\"topbar-right\">");
            sb.append("<button class=\"sort-btn active\" id=\"sortTime\" type=\"button\" aria-label=\"按时间排序\">时间</button>");
            sb.append("<button class=\"sort-btn\" id=\"sortHot\" type=\"button\" aria-label=\"按热度排序\">热度</button>");
            sb.append("</div>");
            sb.append("</div>");
            sb.append("</div>");

            // 左上角浮动点赞和收藏按钮
            sb.append("<div class=\"interaction-buttons-top\" id=\"interactionButtonsTop\" style=\"display:none;\">");
            sb.append("<button class=\"interaction-btn like-btn\" id=\"likeBtnTop\" type=\"button\" aria-label=\"点赞\">");
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3\"></path></svg>");
            sb.append("</button>");
            sb.append("<button class=\"interaction-btn favorite-btn\" id=\"favoriteBtnTop\" type=\"button\" aria-label=\"收藏\">");
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2\"></polygon></svg>");
            sb.append("</button>");
            sb.append("</div>");

            sb.append("<div class=\"page\">");

            sb.append("<div class=\"card post-card\">");

            // 轮播图区域（放在最前面）
            sb.append("<div id=\"carouselWrap\" class=\"carousel\" style=\"display:none;\" aria-label=\"图片轮播\">");
            sb.append("<div id=\"carouselTrack\" class=\"carousel-track\"></div>");
            sb.append("<button class=\"carousel-arrow left\" id=\"carouselLeft\" type=\"button\" aria-label=\"上一张\">‹</button>");
            sb.append("<button class=\"carousel-arrow right\" id=\"carouselRight\" type=\"button\" aria-label=\"下一张\">›</button>");
            sb.append("<div id=\"carouselDots\" class=\"carousel-dots\" aria-label=\"轮播指示器\"></div>");
            sb.append("</div>");

            sb.append("<div class=\"post-header\">");
            sb.append("<div class=\"author-pin\" id=\"authorPin\">");
            sb.append("<div class=\"author-avatar\">").append(authorAvatarHtml).append("</div>");
            sb.append("<div class=\"author-info\">");
            sb.append("<div class=\"author-name\">").append(escapeHtml(authorName)).append("</div>");
            sb.append("<button class=\"follow-btn\" id=\"followBtn\" type=\"button\" style=\"display:none;\">关注</button>");
            sb.append("<button class=\"follow-btn\" id=\"cancelAnonymousBtn\" type=\"button\" style=\"display:none;\">取消匿名</button>");
            sb.append("</div>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append("<div class=\"post-main\">");
            sb.append("<div class=\"post-title\" id=\"postTitle\">").append(safeTitle).append("</div>");
            sb.append("<div id=\"postContent\" class=\"post-content\"></div>");
            
            // 投票区域
            sb.append("<div id=\"voteContainer\" class=\"vote-container\" style=\"display:none;\"></div>");

            sb.append("</div>");

            // 中间位置的点赞和收藏按钮（评论区上方）
            sb.append("<div class=\"interaction-buttons-middle\" id=\"interactionButtonsMiddle\" style=\"display:none;\">");
            sb.append("<button class=\"interaction-btn like-btn\" id=\"likeBtnMiddle\" type=\"button\" aria-label=\"点赞\">");
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3\"></path></svg>");
            sb.append("</button>");
            sb.append("<button class=\"interaction-btn favorite-btn\" id=\"favoriteBtnMiddle\" type=\"button\" aria-label=\"收藏\">");
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2\"></polygon></svg>");
            sb.append("</button>");
            sb.append("</div>");

            sb.append("<div class=\"meta\" id=\"postMeta\">");
            sb.append("<div class=\"topic-tags\" id=\"topicTags\" style=\"display:none;\"></div>");
            sb.append("<div id=\"publishTime\"></div>");
            sb.append("</div>");

            sb.append("</div>");

            sb.append("<div class=\"card comment-card\">");
            sb.append("<div class=\"comment-header\">");
            sb.append("<div class=\"comment-title\">评论</div>");
            sb.append("<div class=\"comment-sort\" aria-label=\"评论排序\">");
            sb.append("<button class=\"sort-btn active\" id=\"sortTime2\" type=\"button\" style=\"display:none\"></button>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append("<div class=\"composer\" aria-label=\"评论输入区\">");
            sb.append("<div class=\"composer-top\">");
            sb.append("<div class=\"composer-left\">");
            sb.append("<div class=\"reply-hint\" id=\"replyHint\">发表一条友善的评论</div>");
            sb.append("</div>");
            sb.append("<div class=\"composer-actions\">");
            sb.append("<button class=\"icon-btn\" id=\"emojiBtn\" type=\"button\" aria-label=\"表情\">表情</button>");
            sb.append("<button class=\"icon-btn\" id=\"cancelReplyBtn\" type=\"button\" aria-label=\"取消回复\">取消</button>");
            sb.append("</div>");
            sb.append("</div>");
            sb.append("<textarea class=\"composer-textarea\" id=\"commentInput\" maxlength=\"500\" placeholder=\"写下你的评论...（Ctrl+Enter 发送）\" aria-label=\"评论内容\"></textarea>");
            sb.append("<div class=\"emoji-panel\" id=\"emojiPanel\"><div class=\"emoji-grid\" id=\"emojiGrid\"></div></div>");
            sb.append("<div class=\"composer-bottom\">");
            sb.append("<div class=\"composer-count\" id=\"commentCount\">0/500</div>");
            sb.append("<button class=\"send-btn\" id=\"sendBtn\" type=\"button\" aria-label=\"发送评论\">发送</button>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append("<div class=\"comment-list\" id=\"commentList\" aria-label=\"评论列表\"></div>");
            sb.append("<div id=\"commentLoading\" style=\"display:none; margin-top: 12px;\">");
            sb.append("<div class=\"skeleton\" style=\"height: 16px; width: 65%; margin: 8px 0;\"></div>");
            sb.append("<div class=\"skeleton\" style=\"height: 16px; width: 55%; margin: 8px 0;\"></div>");
            sb.append("<div class=\"skeleton\" style=\"height: 16px; width: 72%; margin: 8px 0;\"></div>");
            sb.append("</div>");
            sb.append("<div class=\"load-more\"><button id=\"loadMoreBtn\" type=\"button\" style=\"display:none;\">加载更多</button></div>");
            sb.append("</div>");
            sb.append("</div>");

            sb.append("<div class=\"page-skeleton\" id=\"pageSkeleton\" aria-hidden=\"true\">");
            sb.append("<div class=\"skeleton\" style=\"height: 46px; width: 70%; margin: 10px 0;\"></div>");
            sb.append("<div class=\"skeleton\" style=\"height: 260px; width: 100%; margin: 10px 0;\"></div>");
            sb.append("<div class=\"skeleton\" style=\"height: 16px; width: 92%; margin: 10px 0;\"></div>");
            sb.append("<div class=\"skeleton\" style=\"height: 16px; width: 86%; margin: 10px 0;\"></div>");
            sb.append("<div class=\"skeleton\" style=\"height: 16px; width: 78%; margin: 10px 0;\"></div>");
            sb.append("</div>");

            sb.append("<div class=\"image-viewer\" id=\"imageViewer\"><img id=\"imageViewerImg\" alt=\"\"></div>");
            sb.append("<div class=\"toast\" id=\"toast\" role=\"status\" aria-live=\"polite\"></div>");

            sb.append("<script>window.__POST__ = ").append(postJson).append(";</script>");
            sb.append("<script src=\"/js/post-detail.js\"></script>");
            sb.append("</body></html>");
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            // 返回友好的错误页面
            return buildErrorPage("加载帖子失败: " + e.getMessage());
        }
    }

    /**
     * 构建错误页面
     */
    private String buildErrorPage(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html lang=\"zh-CN\">");
        sb.append("<head>");
        sb.append("<meta charset=\"UTF-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        sb.append("<title>错误</title>");
        sb.append("<style>");
        sb.append("body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background: #f5f7fa; }");
        sb.append(".error-container { text-align: center; padding: 40px; background: white; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }");
        sb.append("h1 { color: #f56c6c; margin-bottom: 20px; }");
        sb.append("p { color: #606266; margin-bottom: 30px; }");
        sb.append("a { color: #409eff; text-decoration: none; }");
        sb.append("a:hover { text-decoration: underline; }");
        sb.append("</style>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("<div class=\"error-container\">");
        sb.append("<h1>⚠️ 出错了</h1>");
        sb.append("<p>").append(escapeHtml(message)).append("</p>");
        sb.append("<a href=\"/posts.html\">← 返回帖子列表</a>");
        sb.append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 将PostDetailDto转换为JSON字符串（包含话题名称）
     */
    private String toJsonWithTopics(PostDetailDto dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendJsonNum(sb, "id", dto.getId());
        appendJsonStr(sb, "title", dto.getTitle());
        appendJsonStr(sb, "content", dto.getContent());
        appendJsonStr(sb, "coverImage", dto.getCoverImage());
        appendJsonStr(sb, "category", dto.getCategory());
        appendJsonBool(sb, "isAnonymous", dto.getIsAnonymous());
        appendJsonBool(sb, "favorited", dto.getFavorited());
        appendJsonBool(sb, "liked", dto.getLiked());
        appendJsonNum(sb, "likeCount", dto.getLikeCount());
        appendJsonBool(sb, "canCancelAnonymous", dto.getCanCancelAnonymous());
        appendJsonStr(sb, "postType", dto.getPostType());
        appendJsonNum(sb, "authorId", dto.getAuthorId());
        appendJsonStr(sb, "authorUsername", dto.getAuthorUsername());
        appendJsonStr(sb, "authorAvatar", dto.getAuthorAvatar());
        appendJsonStr(sb, "createTime", dto.getCreateTime() == null ? null : dto.getCreateTime().toString());
        appendJsonStr(sb, "updateTime", dto.getUpdateTime() == null ? null : dto.getUpdateTime().toString());
        
        // 添加topics字段（从topicNames转换）
        sb.append("\"topics\":");
        if (dto.getTopicNames() == null || dto.getTopicNames().isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[");
            int i = 0;
            for (String topicName : dto.getTopicNames()) {
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(escapeJson(topicName)).append("\"}");
                i++;
            }
            sb.append("]");
        }
        sb.append(",");
        
        sb.append("\"images\":");
        if (dto.getImages() == null) {
            sb.append("null");
        } else {
            sb.append("[");
            for (int i = 0; i < dto.getImages().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(dto.getImages().get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append(",");
        
        // 添加vote字段
        sb.append("\"vote\":");
        if (dto.getVote() == null) {
            sb.append("null");
        } else {
            appendVoteToJson(sb, dto.getVote());
        }
        
        sb.append("}");
        return sb.toString();
    }

    private void appendJsonStr(StringBuilder sb, String key, String value) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null,");
        } else {
            sb.append("\"").append(escapeJson(value)).append("\",");
        }
    }

    private void appendJsonNum(StringBuilder sb, String key, Long value) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null,");
        } else {
            sb.append(value).append(",");
        }
    }

    private void appendJsonBool(StringBuilder sb, String key, Boolean value) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null,");
        } else {
            sb.append(value).append(",");
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }

    private String buildDescription(String html) {
        if (html == null) return "";
        String text = html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        if (text.length() > 120) {
            return text.substring(0, 120);
        }
        return text;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeHtmlAttr(String s) {
        return escapeHtml(s).replace("\n", " ").replace("\r", " ");
    }

    /**
     * 将VoteDto转换为JSON字符串
     */
    private void appendVoteToJson(StringBuilder sb, com.example.demo.dto.VoteDto vote) {
        sb.append("{");
        appendJsonNum(sb, "id", vote.getId());
        appendJsonNum(sb, "postId", vote.getPostId());
        appendJsonStr(sb, "title", vote.getTitle());
        appendJsonStr(sb, "deadline", vote.getDeadline() == null ? null : vote.getDeadline().toString());
        appendJsonNumObj(sb, "maxChoices", vote.getMaxChoices());
        appendJsonNumObj(sb, "totalVoters", vote.getTotalVoters());
        appendJsonBool(sb, "isEnded", vote.getIsEnded());
        appendJsonBool(sb, "hasVoted", vote.getHasVoted());
        appendJsonStr(sb, "createTime", vote.getCreateTime() == null ? null : vote.getCreateTime().toString());
        appendJsonStr(sb, "updateTime", vote.getUpdateTime() == null ? null : vote.getUpdateTime().toString());
        
        // 添加options字段
        sb.append("\"options\":");
        if (vote.getOptions() == null || vote.getOptions().isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[");
            for (int i = 0; i < vote.getOptions().size(); i++) {
                if (i > 0) sb.append(",");
                appendVoteOptionToJson(sb, vote.getOptions().get(i));
            }
            sb.append("]");
        }
        
        sb.append("}");
    }

    /**
     * 将VoteOptionDto转换为JSON字符串
     */
    private void appendVoteOptionToJson(StringBuilder sb, com.example.demo.dto.VoteOptionDto option) {
        sb.append("{");
        appendJsonNum(sb, "id", option.getId());
        appendJsonNum(sb, "voteId", option.getVoteId());
        appendJsonStr(sb, "content", option.getContent());
        appendJsonNumObj(sb, "sortOrder", option.getSortOrder());
        appendJsonNumObj(sb, "voteCount", option.getVoteCount());
        appendJsonBool(sb, "voted", option.getVoted());
        appendJsonDouble(sb, "percentage", option.getPercentage());
        sb.append("}");
    }

    private void appendJsonNumObj(StringBuilder sb, String key, Number value) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null,");
        } else {
            sb.append(value).append(",");
        }
    }

    private void appendJsonDouble(StringBuilder sb, String key, Double value) {
        sb.append("\"").append(key).append("\":");
        if (value == null) {
            sb.append("null,");
        } else {
            sb.append(value).append(",");
        }
    }
}
