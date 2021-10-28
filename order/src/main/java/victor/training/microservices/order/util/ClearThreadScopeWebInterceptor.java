package victor.training.microservices.order.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import victor.training.microservices.order.SagaContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClearThreadScopeWebInterceptor implements HandlerInterceptor {
   private final SagaContext context;
   @Override
   public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
      context.flushSaga();
      ClearableThreadScope.clearAllThreadData();
      MDC.clear();
   }

}