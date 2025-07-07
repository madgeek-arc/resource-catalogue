/*
 * Copyright 2017-2025 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.logging;

import gr.uoa.di.madgik.catalogue.config.logging.LogTransactionsFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.User;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class LogUserFilter extends LogTransactionsFilter {

    private static final String TRANSACTION_ID = "transaction_id";
    private static final String USER_INFO = "user_info";
    private static final String REMOTE_IP = "remote_ip";

    @Override
    public void editMDC(MDCAdapter mdc, ServletRequest request, ServletResponse response) {
        String transactionId = UUID.randomUUID().toString();
        mdc.put(TRANSACTION_ID, transactionId);
        appendUserInfo(mdc);
        if (StringUtils.hasText(((HttpServletRequest) request).getHeader("X-Forwarded-For"))) {
            mdc.put(REMOTE_IP, ((HttpServletRequest) request).getHeader("X-Forwarded-For"));
        } else {
            mdc.put(REMOTE_IP, request.getRemoteAddr());
        }
    }

    public static String getTransactionId() {
        return MDC.get(TRANSACTION_ID);
    }

    private void appendUserInfo(MDCAdapter mdc) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            try {
                User user = User.of(authentication);
                if (user != null) {
                    mdc.put(USER_INFO, user.toString());
                }
            } catch (InsufficientAuthenticationException e) {
                mdc.put(USER_INFO, authentication.toString());
            }
        }
    }
}
