package com.xxl.job.workbench.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DataService {

    void transData(int type);

    void transMeta() throws SQLException;

}
