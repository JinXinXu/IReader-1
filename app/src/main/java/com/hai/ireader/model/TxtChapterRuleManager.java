package com.hai.ireader.model;

import com.hai.ireader.DbHelper;
import com.hai.ireader.ReaderApplication;
import com.hai.ireader.bean.TxtChapterRuleBean;
import com.hai.ireader.dao.TxtChapterRuleBeanDao;
import com.hai.ireader.utils.GsonUtils;
import com.hai.ireader.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TxtChapterRuleManager {

    public static List<TxtChapterRuleBean> getAll() {
        List<TxtChapterRuleBean> beans = DbHelper.getDaoSession().getTxtChapterRuleBeanDao().loadAll();
        if (beans.isEmpty()) {
            return getDefault();
        }
        return beans;
    }

    public static List<TxtChapterRuleBean> getEnabled() {
        List<TxtChapterRuleBean> beans = DbHelper.getDaoSession().getTxtChapterRuleBeanDao().queryBuilder()
                .where(TxtChapterRuleBeanDao.Properties.Enable.eq(true))
                .list();
        if (beans.isEmpty()) {
            return getAll();
        }
        return beans;
    }

    public static List<String> enabledRuleList() {
        List<TxtChapterRuleBean> beans = getEnabled();
        List<String> ruleList = new ArrayList<>();
        for (TxtChapterRuleBean chapterRuleBean : beans) {
            ruleList.add(chapterRuleBean.getRule());
        }
        return ruleList;
    }

    public static List<TxtChapterRuleBean> getDefault() {
        String json = null;
        try {
            InputStream inputStream = ReaderApplication.getInstance().getAssets().open("txtChapterRule.json");
            json = IOUtils.toString(inputStream);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<TxtChapterRuleBean> ruleBeanList = GsonUtils.parseJArray(json, TxtChapterRuleBean.class);
        if (ruleBeanList != null) {
            DbHelper.getDaoSession().getTxtChapterRuleBeanDao().insertOrReplaceInTx(ruleBeanList);
            return ruleBeanList;
        }
        return new ArrayList<>();
    }

    public static void del(TxtChapterRuleBean txtChapterRuleBean) {
        DbHelper.getDaoSession().getTxtChapterRuleBeanDao().delete(txtChapterRuleBean);
    }

    public static void del(List<TxtChapterRuleBean> ruleBeanList) {
        for (TxtChapterRuleBean ruleBean : ruleBeanList) {
            del(ruleBean);
        }
    }

    public static void save(TxtChapterRuleBean txtChapterRuleBean) {
        if (txtChapterRuleBean.getSerialNumber() == null) {
            txtChapterRuleBean.setSerialNumber((int) DbHelper.getDaoSession().getTxtChapterRuleBeanDao().queryBuilder().count());
        }
        DbHelper.getDaoSession().getTxtChapterRuleBeanDao().insertOrReplace(txtChapterRuleBean);
    }

    public static void save(List<TxtChapterRuleBean> txtChapterRuleBeans) {
        DbHelper.getDaoSession().getTxtChapterRuleBeanDao().insertOrReplaceInTx(txtChapterRuleBeans);
    }
}
