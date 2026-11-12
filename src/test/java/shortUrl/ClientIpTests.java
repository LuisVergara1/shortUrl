package shortUrl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import shortUrl.configuration.ClientIp;

class ClientIpTests {
    @Test
    void onlyUsesCloudflareHeaderFromAnExplicitlyTrustedPeer() {
        var resolver = new ClientIp("127.0.0.1, 172.18.0.4");
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.1");
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        assertEquals("192.0.2.1", resolver.resolve(request));

        request.setRemoteAddr("172.18.0.4");
        assertEquals("2001:db8::1", resolver.resolve(request));
    }
}
