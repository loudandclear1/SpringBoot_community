package com.hgz.community.entity;

public class Page {
    private int current = 1;
    private int limit = 10;
    private int rows;
    private String path;

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        if (current >= 1) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows >= 0) {
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    //获得当前页的起始行
    public int getOffset() {
        return (current - 1) * limit;
    }

    //获取总页数
    public int getTotal() {
        if(rows % limit == 0) {
            return rows / limit;
        } else {
            return rows / limit + 1;
        }
    }

    public int getFrom() {
        int from = current - 3;
        return Math.max(from, 1);
    }

    public int getTo() {
        int to = current + 3;
        int total = getTotal();
        return Math.min(total, to);
    }
}
