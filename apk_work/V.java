import com.android.apksig.ApkVerifier;
import java.io.File;
public class V {
  public static void main(String[] a) throws Exception {
    ApkVerifier.Result r = new ApkVerifier.Builder(new File(a[0])).build().verify();
    System.out.println("verified = " + r.isVerified());
    System.out.println("errors = " + r.getErrors());
    System.out.println("warnings = " + r.getWarnings());
    System.out.println("v1 = " + r.isVerifiedUsingV1Scheme());
    System.out.println("v2 = " + r.isVerifiedUsingV2Scheme());
    System.out.println("v3 = " + r.isVerifiedUsingV3Scheme());
    System.out.println("certs = " + r.getSignerCertificates());
  }
}
