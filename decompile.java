// 
// Decompiled by Procyon v0.6.0
// 

package org.unlogged.demo.controller;

import io.unlogged.Runtime;
import io.unlogged.logging.Logging;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({ "/customer" })
public class CustomerController
{
    public CustomerController() {
        final int n = 0;
        Logging.recordEvent(1174);
        Logging.recordEvent(n, 1175);
        Logging.recordEvent(1176);
        final int n2 = 0;
        Logging.recordEvent(1177);
        Logging.recordEvent((Object)this, 1178);
        try {
            Logging.recordEvent(1179);
        }
        catch (final Throwable t) {
            Logging.recordEvent(n2, 1181);
            Logging.recordEvent((Object)t, 1182);
            Logging.recordEvent((Object)t, 1183);
            throw t;
        }
    }
    
    public void <init>_SIMPLE();
    
    public float gen_sum(final float a, final float b) {
        final int n = 0;
        try {
            Logging.recordEvent((Object)this, 1185);
            Logging.recordEvent(a, 1186);
            Logging.recordEvent(b, 1187);
            Logging.recordEvent(n, 1188);
            Logging.recordEvent(a, 1189);
            Logging.recordEvent(b, 1190);
            final float n2 = a + b;
            Logging.recordEvent(n2, 1191);
            return n2;
        }
        catch (final Throwable t) {
            Logging.recordEvent(n, 1193);
            Logging.recordEvent((Object)t, 1194);
            Logging.recordEvent((Object)t, 1195);
            throw t;
        }
    }
    
    public float gen_sum_SIMPLE(final float p0, final float p1);
    
    static {
        Runtime.registerClass(new StringBuilder().append("eNqVlHlv0zAUwL2tawVDYvyzfYVOhCTO4TRomUBdK1VM7QRo/yLHcaJMOVCaDrHvwX3f14cEu3WpFdIClp4c+fcuv+c8AMAlAICWF5E6yZI8imigBjTNVZJnZZEnCS3U7mRc5iktur+PmMnun6fqKT7Dwps296Zxb9rCm1brrTnMixQn7Ktt6NBHhmuFeoCwaZiWrfu2idwODCjWqU4xtHUX2kx3/yjHAS1u8Ljk+v9Hna9t7kBLcBZpI/+UknJ6ugtmxTkEoPHop1jC4orY5/yx5G2thj9h+2VxdpVJo8KfCr7BpCXsaRcnyd2H96l3mxIan9FimJeDLC5jnMTnNFAG2bgsJqSM88wbDE9Gt3p3jnvdwc0jZfQgo4VXvZUyxCn19mPm4kA5pGPitfdOpCSeSUlcEEns8CSOcUGz0oPQQco0IdnquWTFZbtytReCN4RUS/NS8M0lpXkleJPJxRr+WuJbNfHfSLxZ4T3G365oLefvKq2t2r+X+LqIIfMPf+Ef+R+z5Glw/klwbnttfo9pF/qS1mehtbFS64vQ2hRxqrl8Fby5JJdvgrdqesH5d4lv1dTqh8TlXqwv+gWaswfK7zJ7nmurpw0bGJZvEotNB8P3Xd23fBsFYWjaBBED62yE2FC3TLLIA7Qimt0bT1Jeina/v9f/hzAQIxSg0PAdGKKO47gmsvSgE5gIdkLLDqFtYkgs5xf3VeSG").toString(), new StringBuilder().append("eNpjYGCZycDAMhuI5wPxIiBeDMTLgXg1ADm+BIc=").toString());
    }
}
