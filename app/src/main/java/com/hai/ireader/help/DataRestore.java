package com.hai.ireader.help;

import android.content.SharedPreferences;

import com.hai.ireader.DbHelper;
import com.hai.ireader.ReaderApplication;
import com.hai.ireader.bean.BookShelfBean;
import com.hai.ireader.bean.BookSourceBean;
import com.hai.ireader.bean.ReplaceRuleBean;
import com.hai.ireader.bean.SearchHistoryBean;
import com.hai.ireader.bean.TxtChapterRuleBean;
import com.hai.ireader.model.BookSourceManager;
import com.hai.ireader.model.ReplaceRuleManager;
import com.hai.ireader.model.TxtChapterRuleManager;
import com.hai.ireader.utils.FileUtils;
import com.hai.ireader.utils.GsonUtils;
import com.hai.ireader.utils.XmlUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by GKF on 2018/1/30.
 * 数据恢复
 */

public class DataRestore {

    public static DataRestore getInstance() {
        return new DataRestore();
    }

    public Boolean run() {
        String dirPath = FileUtils.getSdCardPath() + File.separator + "YueDu";
        restoreConfig(dirPath);
        restoreBookSource(dirPath);
        restoreBookShelf(dirPath);
        restoreSearchHistory(dirPath);
        restoreReplaceRule(dirPath);
        restoreTxtChapterRule(dirPath);
        return true;
    }

    private void restoreConfig(String dirPath) {
        Map<String, ?> entries = null;
        try (FileInputStream ins = new FileInputStream(dirPath + File.separator + "config.xml")) {
            entries = XmlUtils.readMapXml(ins);
        } catch (Exception ignored) {
        }
        if (entries == null || entries.isEmpty()) return;
        long donateHb = ReaderApplication.getConfigPreferences().getLong("DonateHb", 0);
        donateHb = donateHb > System.currentTimeMillis() ? 0 : donateHb;
        SharedPreferences.Editor editor = ReaderApplication.getConfigPreferences().edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            String type = v.getClass().getSimpleName();

            switch (type) {
                case "Integer":
                    editor.putInt(key, (Integer) v);
                    break;
                case "Boolean":
                    editor.putBoolean(key, (Boolean) v);
                    break;
                case "String":
                    editor.putString(key, (String) v);
                    break;
                case "Float":
                    editor.putFloat(key, (Float) v);
                    break;
                case "Long":
                    editor.putLong(key, (Long) v);
                    break;
            }
        }
        editor.putLong("DonateHb", donateHb);
        editor.putInt("versionCode", ReaderApplication.getVersionCode());
        editor.apply();
        ReadBookControl.getInstance().updateReaderSettings();
        ReaderApplication.getInstance().upThemeStore();
        ReaderApplication.getInstance().initNightTheme();
    }

    private void restoreBookShelf(String file) {
        String json = DocumentHelper.readString("myBookShelf.json", file);
        if (json == null) return;
        List<BookShelfBean> bookShelfList = GsonUtils.parseJArray(json, BookShelfBean.class);
        if (bookShelfList == null) return;
        for (BookShelfBean bookshelf : bookShelfList) {
            if (bookshelf.getNoteUrl() != null) {
                DbHelper.getDaoSession().getBookShelfBeanDao().insertOrReplace(bookshelf);
            }
            if (bookshelf.getBookInfoBean().getNoteUrl() != null) {
                DbHelper.getDaoSession().getBookInfoBeanDao().insertOrReplace(bookshelf.getBookInfoBean());
            }
        }
    }

    private void restoreBookSource(String file) {
        String json = DocumentHelper.readString("myBookSource.json", file);
        if (json == null) return;
        List<BookSourceBean> bookSourceBeans = GsonUtils.parseJArray(json, BookSourceBean.class);
        if (bookSourceBeans == null) return;
        BookSourceManager.addBookSource(bookSourceBeans);
    }

    private void restoreSearchHistory(String file) {
        String json = DocumentHelper.readString("myBookSearchHistory.json", file);
        if (json == null) return;
        List<SearchHistoryBean> searchHistoryBeans = GsonUtils.parseJArray(json, SearchHistoryBean.class);
        if (searchHistoryBeans == null) return;
        DbHelper.getDaoSession().getSearchHistoryBeanDao().insertOrReplaceInTx(searchHistoryBeans);
    }

    private void restoreReplaceRule(String file) {
        String json = DocumentHelper.readString("myBookReplaceRule.json", file);
        if (json == null) return;
        List<ReplaceRuleBean> replaceRuleBeans = GsonUtils.parseJArray(json, ReplaceRuleBean.class);
        if (replaceRuleBeans == null) return;
        ReplaceRuleManager.addDataS(replaceRuleBeans);
    }

    private void restoreTxtChapterRule(String file) {
        String json = DocumentHelper.readString("myTxtChapterRule.json", file);
        if (json == null) return;
        List<TxtChapterRuleBean> ruleBeanList = GsonUtils.parseJArray(json, TxtChapterRuleBean.class);
        if (ruleBeanList == null) return;
        TxtChapterRuleManager.save(ruleBeanList);
    }
}
