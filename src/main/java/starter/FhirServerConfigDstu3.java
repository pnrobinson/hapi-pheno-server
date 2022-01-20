package starter;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.lastn.ElasticsearchSvcImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import starter.annotations.OnDSTU3Condition;
import starter.cql.StarterCqlDstu3Config;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@Conditional(OnDSTU3Condition.class)
@Import(StarterCqlDstu3Config.class)
public class FhirServerConfigDstu3 extends BaseJavaConfigDstu3 {

  @Autowired
  private DataSource myDataSource;

  /**
   * We override the paging provider definition so that we can customize
   * the default/max page sizes for search results. You can set these however
   * you want, although very large page sizes will require a lot of RAM.
   */
  @Autowired
  AppProperties appProperties;

  @PostConstruct
  public void initSettings() {
    if(appProperties.getSearch_coord_core_pool_size() != null) {
		 setSearchCoordCorePoolSize(appProperties.getSearch_coord_core_pool_size());
	 }
	  if(appProperties.getSearch_coord_max_pool_size() != null) {
		  setSearchCoordMaxPoolSize(appProperties.getSearch_coord_max_pool_size());
	  }
	  if(appProperties.getSearch_coord_queue_capacity() != null) {
		  setSearchCoordQueueCapacity(appProperties.getSearch_coord_queue_capacity());
	  }
  }


  @Override
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
    pagingProvider.setDefaultPageSize(appProperties.getDefault_page_size());
    pagingProvider.setMaximumPageSize(appProperties.getMax_page_size());
    return pagingProvider;
  }

  @Autowired
  private ConfigurableEnvironment configurableEnvironment;
  
  @Override
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
	  ConfigurableListableBeanFactory myConfigurableListableBeanFactory) {
    LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory(myConfigurableListableBeanFactory);
    retVal.setPersistenceUnitName("HAPI_PU");

    try {
      retVal.setDataSource(myDataSource);
    } catch (Exception e) {
      throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
    }

    retVal.setJpaProperties(EnvironmentHelper.getHibernateProperties(configurableEnvironment,
		 myConfigurableListableBeanFactory));
    return retVal;
  }

  @Bean
  @Primary
  public JpaTransactionManager hapiTransactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  @Bean()
  public ElasticsearchSvcImpl elasticsearchSvc(PartitionSettings thePartitionSetings) {
	  if (Boolean.TRUE.equals(EnvironmentHelper.isElasticsearchEnabled(configurableEnvironment))) {
		  String elasticsearchUrl = EnvironmentHelper.getElasticsearchServerUrl(configurableEnvironment);
		  String elasticsearchHost = elasticsearchUrl;
		  String elasticsearchProtocol = EnvironmentHelper.getElasticsearchServerProtocol(configurableEnvironment);
		  if (elasticsearchUrl.startsWith("http")) {
			  elasticsearchProtocol = elasticsearchUrl.split("://")[0];
			  elasticsearchHost = elasticsearchUrl.split("://")[1];
		  }
		  String elasticsearchUsername = EnvironmentHelper.getElasticsearchServerUsername(configurableEnvironment);
		  String elasticsearchPassword = EnvironmentHelper.getElasticsearchServerPassword(configurableEnvironment);
		  return new ElasticsearchSvcImpl(thePartitionSetings, elasticsearchUrl, elasticsearchUsername, elasticsearchPassword);
	  } else {
		  return null;
	  }
  }

}
