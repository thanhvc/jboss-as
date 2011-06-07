package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.CommonAdminObject;
import org.jboss.jca.common.api.metadata.common.CommonConnDef;
import org.jboss.jca.common.api.metadata.common.CommonPool;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonTimeOut;
import org.jboss.jca.common.api.metadata.common.CommonValidation;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.common.FlushStrategy;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.common.TransactionSupportEnum;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.common.CommonAdminObjectImpl;
import org.jboss.jca.common.metadata.common.CommonConnDefImpl;
import org.jboss.jca.common.metadata.common.CommonPoolImpl;
import org.jboss.jca.common.metadata.common.CommonSecurityImpl;
import org.jboss.jca.common.metadata.common.CommonTimeOutImpl;
import org.jboss.jca.common.metadata.common.CommonValidationImpl;
import org.jboss.jca.common.metadata.common.CredentialImpl;
import org.jboss.jca.common.metadata.resourceadapter.ResourceAdapterImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.connector.pool.Constants.*;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.*;

public abstract class AbstractRaOperation {

    protected ResourceAdapter buildResourceAdapterObject(ModelNode raNode) throws OperationFailedException {


        Map<String, String> configProperties = new HashMap<String, String>();
        if (raNode.has(CONFIG_PROPERTIES) && raNode.get(CONFIG_PROPERTIES).isDefined()) {
            for (ModelNode property : raNode.get(CONFIG_PROPERTIES).asList()) {
                configProperties.put(property.asProperty().getName(), property.asString());
            }
        }
        String archive = getStringIfSetOrGetDefault(raNode, ARCHIVE, null);
        TransactionSupportEnum transactionSupport = raNode.hasDefined(TRANSACTIONSUPPORT) ? TransactionSupportEnum
                .valueOf(raNode.get(TRANSACTIONSUPPORT).asString()) : null;
        String bootstrapContext = getStringIfSetOrGetDefault(raNode, BOOTSTRAPCONTEXT, null);
        List<String> beanValidationGroups = new ArrayList<String>();
        if (raNode.has(BEANVALIDATIONGROUPS) && raNode.get(BEANVALIDATIONGROUPS).isDefined()) {
            for (ModelNode beanValidtion : raNode.get(BEANVALIDATIONGROUPS).asList()) {
                beanValidationGroups.add(beanValidtion.asString());
            }
        }
        ResourceAdapter ra;
        try {
            ra = new ResourceAdapterImpl(archive, transactionSupport, buildConnectionDefinitionObject(raNode),
                    buildAdminObjects(raNode), configProperties, beanValidationGroups, bootstrapContext);
        } catch (ValidateException e) {
            throw new OperationFailedException(e, raNode);
        }
        return ra;
    }

    private List<CommonConnDef> buildConnectionDefinitionObject(ModelNode parentNode) throws ValidateException {
        List<CommonConnDef> connDefs = new ArrayList<CommonConnDef>();

        for (ModelNode conDefNode : parentNode.get(CONNECTIONDEFINITIONS).asList()) {
            Map<String, String> configProperties = new HashMap<String, String>();
            if (conDefNode.has(CONFIG_PROPERTIES) && conDefNode.get(CONFIG_PROPERTIES).isDefined()) {
                for (ModelNode property : conDefNode.get(CONFIG_PROPERTIES).asList()) {
                    configProperties.put(property.asProperty().getName(), property.asString());
                }
            }
            String className = getStringIfSetOrGetDefault(conDefNode, CLASS_NAME, null);
            String jndiName = getStringIfSetOrGetDefault(conDefNode, JNDI_NAME, null);
            String poolName = getStringIfSetOrGetDefault(conDefNode, POOL_NAME, null);
            boolean enabled = getBooleanIfSetOrGetDefault(conDefNode, ENABLED, false);
            boolean useJavaContext = getBooleanIfSetOrGetDefault(conDefNode, USE_JAVA_CONTEXT, false);
            boolean useCcm = getBooleanIfSetOrGetDefault(conDefNode, USE_CCM, true);

            Integer maxPoolSize = getIntIfSetOrGetDefault(conDefNode, MAX_POOL_SIZE, null);
            Integer minPoolSize = getIntIfSetOrGetDefault(conDefNode, MIN_POOL_SIZE, null);
            boolean prefill = getBooleanIfSetOrGetDefault(conDefNode, POOL_PREFILL, false);
            boolean useStrictMin = getBooleanIfSetOrGetDefault(conDefNode, POOL_USE_STRICT_MIN, false);
            final FlushStrategy flushStrategy = conDefNode.hasDefined(FLUSH_STRATEGY) ? FlushStrategy.valueOf(conDefNode.get(
                    FLUSH_STRATEGY).asString()) : FlushStrategy.FAILING_CONNECTION_ONLY;

            Integer allocationRetry = getIntIfSetOrGetDefault(conDefNode, ALLOCATION_RETRY, null);
            Long allocationRetryWaitMillis = getLongIfSetOrGetDefault(conDefNode, ALLOCATION_RETRY_WAIT_MILLIS, null);
            Long blockingTimeoutMillis = getLongIfSetOrGetDefault(conDefNode, BLOCKING_TIMEOUT_WAIT_MILLIS, null);
            Long idleTimeoutMinutes = getLongIfSetOrGetDefault(conDefNode, IDLETIMEOUTMINUTES, null);
            Integer xaResourceTimeout = getIntIfSetOrGetDefault(conDefNode, XA_RESOURCE_TIMEOUT, null);
            CommonTimeOut timeOut = new CommonTimeOutImpl(blockingTimeoutMillis, idleTimeoutMinutes, allocationRetry,
                    allocationRetryWaitMillis, xaResourceTimeout);
            CommonPool pool = new CommonPoolImpl(minPoolSize, maxPoolSize, prefill, useStrictMin, flushStrategy);

            String securityDomain = getStringIfSetOrGetDefault(conDefNode, SECURITY_DOMAIN, null);
            String securityDomainAndApplication = getStringIfSetOrGetDefault(conDefNode, SECURITY_DOMAIN_AND_APPLICATION, null);
            boolean application = getBooleanIfSetOrGetDefault(conDefNode, APPLICATION, false);
            CommonSecurity security = null;
            if (!isEmpty(securityDomain) || !isEmpty(securityDomainAndApplication)) {
                security = new CommonSecurityImpl(securityDomain, securityDomainAndApplication, application);
            }

            Long backgroundValidationMinutes = getLongIfSetOrGetDefault(conDefNode, BACKGROUNDVALIDATIONMINUTES, null);
            boolean backgroundValidation = getBooleanIfSetOrGetDefault(conDefNode, BACKGROUNDVALIDATION, false);
            boolean useFastFail = getBooleanIfSetOrGetDefault(conDefNode, USE_FAST_FAIL, false);
            CommonValidation validation = new CommonValidationImpl(backgroundValidation, backgroundValidationMinutes,
                    useFastFail);
            final String recoveryUsername = getStringIfSetOrGetDefault(conDefNode, RECOVERY_USERNAME, null);
            final String recoveryPassword = getStringIfSetOrGetDefault(conDefNode, RECOVERY_PASSWORD, null);
            final String recoverySecurityDomain = getStringIfSetOrGetDefault(conDefNode, RECOVERY_SECURITY_DOMAIN, null);

            final Credential credential = new CredentialImpl(recoveryUsername, recoveryPassword, recoverySecurityDomain);

            final Extension recoverPlugin = extractExtension(conDefNode, RECOVERLUGIN_CLASSNAME, RECOVERLUGIN_PROPERTIES);
            final boolean noRecovery = getBooleanIfSetOrGetDefault(conDefNode, NO_RECOVERY, false);
            Recovery recovery = new Recovery(credential, recoverPlugin, noRecovery);
            CommonConnDef connectionDefinition = new CommonConnDefImpl(configProperties, className, jndiName, poolName,
                    enabled, useJavaContext, useCcm, pool, timeOut, validation, security, recovery);

            connDefs.add(connectionDefinition);
        }
        return connDefs;
    }

    private boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    private List<CommonAdminObject> buildAdminObjects(ModelNode parentNode) {
        List<CommonAdminObject> adminObjets = new ArrayList<CommonAdminObject>();
        if (parentNode.has(ADMIN_OBJECTS) && parentNode.get(ADMIN_OBJECTS).isDefined()) {
            for (ModelNode adminObject : parentNode.get(ADMIN_OBJECTS).asList()) {
                Map<String, String> configProperties = new HashMap<String, String>(adminObject.get(CONFIG_PROPERTIES).asList()
                        .size());
                if (adminObject.has(CONFIG_PROPERTIES)) {
                    for (ModelNode property : adminObject.get(CONFIG_PROPERTIES).asList()) {
                        configProperties.put(property.asProperty().getName(), property.asString());
                    }
                }
                String className = getStringIfSetOrGetDefault(adminObject, CLASS_NAME, null);
                String jndiName = getStringIfSetOrGetDefault(adminObject, JNDI_NAME, null);
                String poolName = getStringIfSetOrGetDefault(adminObject, POOL_NAME, null);
                boolean enabled = getBooleanIfSetOrGetDefault(adminObject, ENABLED, false);
                boolean useJavaContext = getBooleanIfSetOrGetDefault(adminObject, USE_JAVA_CONTEXT, false);

                CommonAdminObject adminObjet = new CommonAdminObjectImpl(configProperties, className, jndiName, poolName, enabled,
                        useJavaContext);

                adminObjets.add(adminObjet);
            }
        }
        return adminObjets;
    }

    private Long getLongIfSetOrGetDefault(ModelNode dataSourceNode, String key, Long defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asLong();
        } else {
            return defaultValue;
        }
    }

    private Integer getIntIfSetOrGetDefault(ModelNode dataSourceNode, String key, Integer defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asInt();
        } else {
            return defaultValue;
        }
    }

    private boolean getBooleanIfSetOrGetDefault(ModelNode dataSourceNode, String key, boolean defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asBoolean();
        } else {
            return defaultValue;
        }
    }

    private String getStringIfSetOrGetDefault(ModelNode dataSourceNode, String key, String defaultValue) {
        if (dataSourceNode.hasDefined(key)) {
            return dataSourceNode.get(key).asString();
        } else {
            return defaultValue;
        }
    }

    private Extension extractExtension(final ModelNode node, final String className, final String propertyName)
            throws ValidateException {
        if (node.hasDefined(className)) {
            String exceptionSorterClassName = node.get(className).asString();

            getStringIfSetOrGetDefault(node, className, null);

            Map<String, String> exceptionSorterProperty = null;
            if (node.hasDefined(propertyName)) {
                exceptionSorterProperty = new HashMap<String, String>(node.get(propertyName).asList().size());
                for (ModelNode property : node.get(propertyName).asList()) {
                    exceptionSorterProperty.put(property.asProperty().getName(), property.asString());
                }
            }

            return new Extension(exceptionSorterClassName, exceptionSorterProperty);
        } else {
            return null;
        }
    }

}