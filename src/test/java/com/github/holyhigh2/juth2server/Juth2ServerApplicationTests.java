package com.github.holyhigh2.juth2server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.hamcrest.core.StringContains.containsString;

@SpringBootTest
@AutoConfigureMockMvc
class Juth2ServerApplicationTests {
	@Autowired
	private MockMvc mvc;

	@Test
	void authorizeWithoutHeaders() throws Exception {
		mvc.perform(MockMvcRequestBuilders
				.request(HttpMethod.POST,"/juth2-demo/authorize")
				)
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.status().reason(containsString("No Authorization header was found")))
				.andDo(MockMvcResultHandlers.print());
	}

	@Test
	void requestResourceWithoutAuthentication() throws Exception {
		mvc.perform(MockMvcRequestBuilders
				.request(HttpMethod.POST,"/a/b")
		)
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andDo(MockMvcResultHandlers.print());
	}
}
