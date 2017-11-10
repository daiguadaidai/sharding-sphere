/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package io.shardingjdbc.orchestration.internal.state.datasource;

import io.shardingjdbc.core.api.config.MasterSlaveRuleConfiguration;
import io.shardingjdbc.core.rule.MasterSlaveRule;
import io.shardingjdbc.orchestration.api.config.OrchestrationConfiguration;
import io.shardingjdbc.orchestration.internal.config.ConfigurationService;
import io.shardingjdbc.orchestration.internal.state.StateNode;
import io.shardingjdbc.orchestration.internal.state.StateNodeStatus;
import io.shardingjdbc.orchestration.reg.base.CoordinatorRegistryCenter;
import lombok.Getter;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Data source service.
 * 
 * @author caohao
 */
@Getter
public final class DataSourceService {
    
    private final String dataSourceNodePath;
    
    private final CoordinatorRegistryCenter regCenter;
    
    private final ConfigurationService configurationService;
    
    private final String name;
    
    public DataSourceService(final OrchestrationConfiguration config) {
        dataSourceNodePath = new StateNode(config.getName()).getDataSourcesNodeFullPath();
        regCenter = config.getRegistryCenter();
        configurationService = new ConfigurationService(config);
        name = config.getName();
    }
    
    /**
     * Persist master-salve data sources node.
     *
     */
    public void persistDataSourcesNode() {
        regCenter.persist(dataSourceNodePath, "");
        regCenter.addCacheData(dataSourceNodePath);
    }
    
    public String getDataSourceNodePath() {
        return dataSourceNodePath;
    }
    
    public MasterSlaveRule getAvailableMasterSlaveRule() {
        Map<String, DataSource> dataSourceMap = configurationService.loadDataSourceMap();
        String dataSourcesNodePath = new StateNode(name).getDataSourcesNodeFullPath();
        List<String> dataSources = regCenter.getChildrenKeys(dataSourcesNodePath);
        MasterSlaveRuleConfiguration ruleConfig = configurationService.loadMasterSlaveRuleConfiguration();
        for (String each : dataSources) {
            String dataSourceName = each.substring(each.lastIndexOf("/") + 1);
            String path = dataSourcesNodePath + "/" + each;
            if (StateNodeStatus.DISABLED.toString().equalsIgnoreCase(regCenter.get(path)) && dataSourceMap.containsKey(dataSourceName)) {
                dataSourceMap.remove(dataSourceName);
                ruleConfig.getSlaveDataSourceNames().remove(dataSourceName);
            }
        }
        return ruleConfig.build(dataSourceMap);
    }
}