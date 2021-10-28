/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package victor.training.microservices.order.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Silly but correct implementation of Thread scope that supports clearing of all data at the end.
 * Copy of
 * @see org.springframework.context.support.SimpleThreadScope
 */
@Slf4j
public class ClearableThreadScope implements Scope {

	private static final ThreadLocal<Map<String, Object>> threadScope =
		new NamedThreadLocal<>("SimpleThreadScope") {
			@Override
			protected Map<String, Object> initialValue() {
				return new HashMap<>();
			}
		};

	public static void clearAllThreadData() {
		threadScope.remove();
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Map<String, Object> scope = threadScope.get();
		Object scopedObject = scope.get(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scope.put(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		Map<String, Object> scope = threadScope.get();
		return scope.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		log.warn("SimpleThreadScope does not support destruction callbacks. " +
					"Consider using RequestScope in a web environment.");
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return Thread.currentThread().getName();
	}

}
