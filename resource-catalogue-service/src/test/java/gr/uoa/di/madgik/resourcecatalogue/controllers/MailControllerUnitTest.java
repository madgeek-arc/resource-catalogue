/*
 * Copyright 2017-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.resourcecatalogue.controllers;

import gr.uoa.di.madgik.resourcecatalogue.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MailControllerUnitTest {

    private MailController controller;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        controller = spy(new MailController());
        mailService = mock(MailService.class);
        controller.mailService = mailService;
    }

    @Test
    void sendToAll_sendsBccInChunksSizedToLeaveRoomForCc() throws Exception {
        List<String> recipients = emails(205);
        List<String> cc = List.of("cc1@example.com", "cc2@example.com");
        doReturn(recipients).when(controller).getAllEmails(anyBoolean(), anyBoolean());

        controller.sendToAll(cc, "subject", false, false, "text");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> bcc = ArgumentCaptor.forClass(List.class);
        verify(mailService, times(3)).sendMail(eq(List.of()), eq(cc), bcc.capture(), eq("subject"), eq("text"));
        assertThat(bcc.getAllValues()).extracting(List::size).containsExactly(98, 98, 9);
        assertThat(bcc.getAllValues().stream().flatMap(List::stream).toList()).isEqualTo(recipients);
    }

    @Test
    void sendToAll_sendsNothingWhenThereAreNoRecipients() throws Exception {
        doReturn(List.of()).when(controller).getAllEmails(anyBoolean(), anyBoolean());

        controller.sendToAll(List.of(), "subject", false, false, "text");

        verify(mailService, never()).sendMail(anyList(), anyList(), anyList(), anyString(), anyString());
    }

    @Test
    void sendToAll_rejectsCcListThatLeavesNoRoomForBcc() throws Exception {
        assertThatThrownBy(() -> controller.sendToAll(emails(100), "subject", false, false, "text"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(controller, never()).getAllEmails(any(), any());
        verify(mailService, never()).sendMail(anyList(), anyList(), anyList(), anyString(), anyString());
    }

    private static List<String> emails(int count) {
        return IntStream.range(0, count).mapToObj(i -> "user" + i + "@example.com").toList();
    }
}
