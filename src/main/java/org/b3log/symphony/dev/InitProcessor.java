/*
 * Copyright (c) 2012, B3log Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.symphony.dev;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.model.Role;
import org.b3log.latke.model.User;
import org.b3log.latke.repository.Transaction;
import org.b3log.latke.repository.jdbc.util.JdbcRepositories;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.symphony.model.Article;
import org.b3log.symphony.model.Option;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.repository.OptionRepository;
import org.b3log.symphony.service.ArticleMgmtService;
import org.b3log.symphony.service.UserMgmtService;
import org.b3log.symphony.service.UserQueryService;
import org.json.JSONObject;

/**
 * Initializes database.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.8, Nov 22, 2012
 * @since 0.2.0
 */
@RequestProcessor
public class InitProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(InitProcessor.class.getName());
    /**
     * Number of article to generate.
     */
    private static final int ARTICLE_GENERATE_NUM = 49;

    /**
     * Generates tables.
     * 
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws IOException io exception 
     */
    @RequestProcessing(value = "/dev/db/table/gen", method = HTTPRequestMethod.GET)
    public void genTables(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        try {
            LOGGER.log(Level.INFO, "Database [{0}], creates all tables", Latkes.getRuntimeDatabase());

            final List<JdbcRepositories.CreateTableResult> createTableResults = JdbcRepositories.initAllTables();
            for (final JdbcRepositories.CreateTableResult createTableResult : createTableResults) {
                LOGGER.log(Level.INFO, "Creates table result[tableName={0}, isSuccess={1}]",
                        new Object[]{createTableResult.getName(), createTableResult.isSuccess()});
            }

            // Init stat.
            final OptionRepository optionRepository = OptionRepository.getInstance();
            final Transaction transaction = optionRepository.beginTransaction();

            JSONObject option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_STATISTIC_MEMBER_COUNT);
            option.put(Option.OPTION_VALUE, "0");
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_STATISTIC);
            optionRepository.add(option);

            option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_STATISTIC_CMT_COUNT);
            option.put(Option.OPTION_VALUE, "0");
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_STATISTIC);
            optionRepository.add(option);

            option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_STATISTIC_ARTICLE_COUNT);
            option.put(Option.OPTION_VALUE, "0");
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_STATISTIC);
            optionRepository.add(option);

            option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_STATISTIC_TAG_COUNT);
            option.put(Option.OPTION_VALUE, "0");
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_STATISTIC);
            optionRepository.add(option);
            
            option = new JSONObject();
            option.put(Keys.OBJECT_ID, Option.ID_C_STATISTIC_MAX_ONLINE_VISITOR_COUNT);
            option.put(Option.OPTION_VALUE, "0");
            option.put(Option.OPTION_CATEGORY, Option.CATEGORY_C_STATISTIC);
            optionRepository.add(option);

            transaction.commit();

            // Init admin
            final UserMgmtService userMgmtService = UserMgmtService.getInstance();
            JSONObject admin = new JSONObject();
            admin.put(User.USER_EMAIL, UserExt.DEFAULT_ADMIN_EMAIL);
            admin.put(User.USER_NAME, UserExt.DEFAULT_ADMIN_NAME);
            admin.put(User.USER_PASSWORD, "test");
            admin.put(User.USER_ROLE, Role.ADMIN_ROLE);
            userMgmtService.addUser(admin);

            // Init default commenter (for sync comment from client)
            final JSONObject defaultCommenter = new JSONObject();
            defaultCommenter.put(User.USER_EMAIL, UserExt.DEFAULT_CMTER_EMAIL);
            defaultCommenter.put(User.USER_NAME, UserExt.DEFAULT_CMTER_NAME);
            defaultCommenter.put(User.USER_PASSWORD, String.valueOf(new Random().nextInt()));
            defaultCommenter.put(User.USER_ROLE, UserExt.DEFAULT_CMTER_ROLE);
            userMgmtService.addUser(defaultCommenter);

            // Hello World!
            admin = UserQueryService.getInstance().getAdmin();
            final ArticleMgmtService articleMgmtService = ArticleMgmtService.getInstance();
            final JSONObject article = new JSONObject();
            article.put(Article.ARTICLE_TITLE, "B3log 社区上线 &hearts;");
            article.put(Article.ARTICLE_TAGS, "B3log, Java, Q&A, B3log Announcement");
            article.put(Article.ARTICLE_CONTENT, "看 [About](/about) 了解一下这里吧 ;-p");
            article.put(Article.ARTICLE_EDITOR_TYPE, 0);
            article.put(Article.ARTICLE_AUTHOR_EMAIL, admin.optString(User.USER_EMAIL));
            article.put(Article.ARTICLE_AUTHOR_ID, admin.optString(Keys.OBJECT_ID));
            articleMgmtService.addArticle(article);

            response.sendRedirect("/");
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Creates database tables failed", e);
            throw new IOException("Creates database tables failed", e);
        }
    }

    /**
     * Generates mock articles.
     * 
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws IOException io exception 
     */
    @RequestProcessing(value = "/dev/article/gen", method = HTTPRequestMethod.GET)
    public void genArticles(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        try {
            final ArticleMgmtService articleMgmtService = ArticleMgmtService.getInstance();
            final UserQueryService userQueryService = UserQueryService.getInstance();
            final JSONObject admin = userQueryService.getAdmin();
            final OptionRepository optionRepository = OptionRepository.getInstance();
            final JSONObject articleCntOption = optionRepository.get(Option.ID_C_STATISTIC_ARTICLE_COUNT);
            final int start = articleCntOption.optInt(Option.OPTION_VALUE) + 1;
            final int end = start + ARTICLE_GENERATE_NUM;

            for (int i = start; i <= end; i++) {
                final JSONObject article = new JSONObject();
                article.put(Article.ARTICLE_TITLE, "你好，世界！ (" + i + ')');
                article.put(Article.ARTICLE_TAGS, "B3log, Java, " + i);
                article.put(Article.ARTICLE_CONTENT, "测试正文");
                article.put(Article.ARTICLE_EDITOR_TYPE, 0);
                article.put(Article.ARTICLE_AUTHOR_EMAIL, admin.optString(User.USER_EMAIL));
                article.put(Article.ARTICLE_AUTHOR_ID, admin.optString(Keys.OBJECT_ID));
                articleMgmtService.addArticle(article);

                LOGGER.log(Level.INFO, "Generated article ({0})", i);
            }

            response.sendRedirect("/");
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Creates database tables failed", e);
            throw new IOException("Creates database tables failed", e);
        }
    }
}
