package karlkfi.spring.web.method.annotation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;

import com.google.common.collect.ImmutableList;

/**
 * Integration Tests of AutowiredArgumentResolver with AutowiredParam annotations on a Spring MVC Controller.
 * Tests many aspects of Spring MVC configuration with a custom HandlerMethodArgumentResolver.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AutowiredParamConfig.class)
public class AutowiredParamControllerTest {
	
	private MockMvc mockMvc;
	
	@Autowired
    private WebApplicationContext wac;
	
	@Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }
	
	/**
	 * Tests that a required application-scoped bean can be injected into controller request method params
	 */
	@Test
	public void testRequest() throws Exception {
		this.mockMvc.perform(get("/request").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attribute("text", "injected-text"));
	}
	
	/**
	 * Tests that autowired beans are injected by type, not name.
	 */
	@Test
	public void testRequestMissing() throws Throwable {
		this.mockMvc.perform(get("/requestMissing").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attribute("text", "injected-text"));
	}
	
	/**
	 * Tests that a missing optional resource can be null
	 */
	@Test
	public void testRequestOptional() throws Exception {
		this.mockMvc.perform(get("/requestOptional").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attribute("set", (Object) null));
	}
	
	/**
	 * Tests that request-scoped dates should not be the same across multiple requests
	 */
	@Test
	public void testRequestDate() throws Exception {
		MvcResult result1 = this.mockMvc.perform(get("/requestDate").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("date"))
				.andReturn();
		
		Date date1 = (Date) result1.getModelAndView().getModel().get("date");
		
		MvcResult result2 = this.mockMvc.perform(get("/requestDate").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("date"))
				.andReturn();
			
		Date date2 = (Date) result2.getModelAndView().getModel().get("date");
		
		assertThat(date1, not(equalTo(date2)));
	}
	
	/**
	 * RequestMappingHandlerAdapter's default ordering puts MapMethodProcessor ahead of custom Argument Resolvers.
	 * So we can't actually inject Maps from the context. :(
	 */
	@Test
	public void testRequestMap() throws Exception {
		MvcResult result1 = this.mockMvc.perform(get("/requestMap").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("map"))
				.andReturn();
		
		BindingAwareModelMap map1 = (BindingAwareModelMap) result1.getModelAndView().getModel().get("map");
		assertThat((String)map1.get("param"), equalTo("test"));
	}
	
	/**
	 * Tests that parent types can't be autowired by type with multiple children in the context
	 */
	@Test(expected=NoSuchBeanDefinitionException.class)
	public void testRequestArrayList() throws Throwable {
		try {
			this.mockMvc.perform(get("/requestArrayList").param("param", "test"))
					//.andDo(print())
					.andExpect(status().isOk())
					.andExpect(model().attribute("param", "test"))
					.andExpect(model().attributeExists("list"));
		} catch (NestedServletException e) {
			throw e.getCause();
		}
	}
	
	/**
	 * Tests that prototype-scoped maps should not be the same across multiple requests
	 * Also tests ResourceParam's type and name options.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testRequestImmutableList() throws Exception {
		MvcResult result1 = this.mockMvc.perform(get("/requestImmutableList").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("list"))
				.andReturn();
		
		ImmutableList<String> list1 = (ImmutableList<String>) result1.getModelAndView().getModel().get("list");
		
		MvcResult result2 = this.mockMvc.perform(get("/requestImmutableList").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("list"))
				.andReturn();
			
		ImmutableList<String> list2 = (ImmutableList<String>) result2.getModelAndView().getModel().get("list");
				
		assertThat(list1, not(equalTo(list2)));
	}
	
	/**
	 * Tests that Parent types can be autowired with a Child instance
	 */
	@Test
	public void testRequestParentChild() throws Exception {
		MvcResult result1 = this.mockMvc.perform(get("/requestChild").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("child"))
				.andReturn();
		
		Child child1 = (Child) result1.getModelAndView().getModel().get("child");
		
		MvcResult result2 = this.mockMvc.perform(get("/requestParent").param("param", "test"))
				//.andDo(print())
				.andExpect(status().isOk())
				.andExpect(model().attribute("param", "test"))
				.andExpect(model().attributeExists("parent"))
				.andReturn();
			
		Parent parent1 = (Parent) result2.getModelAndView().getModel().get("parent");
				
		assertThat(child1, sameInstance(parent1));
	}

}
