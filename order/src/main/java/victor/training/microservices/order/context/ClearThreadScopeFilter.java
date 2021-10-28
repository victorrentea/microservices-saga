package victor.training.microservices.order.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClearThreadScopeFilter implements Filter {
   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      try {
         chain.doFilter(request, response);
      } finally {
         ClearableThreadScope.clearAllThreadData();
      }
   }
}