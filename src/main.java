/**
* @ Ayberk Karaköse 
* @ 10.03.2024 Programın yazıldığı tarih
* Github depo kopyalama ve analizi
*/

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class main {
    public static void main(String[] args) {
        try {
            // Kullanıcıdan Depo URL'sini al
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Depo URL'sini giriniz: ");
            String depoUrl = reader.readLine();

            // Klonlama işlemi için klonlanacak dizini belirleme
            String anaKlasorAdi = "klondepo";
            String klonKlasorAdi = sonrakiGecersizKlasorAdiniAl(anaKlasorAdi, depoUrl);
            File klonKlasoru = new File(System.getProperty("user.dir"), anaKlasorAdi + "/" + klonKlasorAdi);

            // Klonlanan depo varsa sil
            if (klonKlasoru.exists()) {
                klasoruSil(klonKlasoru);
            }

            // Klonlanan depo için alt klasör oluşturma
            klonKlasoru.mkdir();

            // Git klonlama işlemi
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("git", "clone", depoUrl, klonKlasoru.getPath());
            Process process = processBuilder.start();

            // Klonlama işleminin sonucunu kontrol etme
            int cikisKodu = process.waitFor();
            if (cikisKodu == 0) {
                System.out.println("Depo başarıyla klonlandı.");
                System.out.println( "Klonlanan dizin: " + klonKlasoru.getAbsolutePath());
                // .java dosyalarını bulma ve sadece sınıf içeren dosyaları listeleme
                List<File> sinifDosyalari = sadeceSinifDosyalariBul(klonKlasoru);
                System.out.println("Analiz sonuçları:");
                for (File dosya : sinifDosyalari) {
                    javaDosyasiniIncele(dosya);
                }
            } else {
                System.out.println("Depo klonlanırken bir hata oluştu. Hata kodu: " + cikisKodu);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Boş bir dizin adı bulma metodu
    private static String sonrakiGecersizKlasorAdiniAl(String anaKlasorAdi, String depoUrl) {
        String klasorAdi = getDepoAdi(depoUrl) + "_klon";
        File klasor = new File(System.getProperty("user.dir"), anaKlasorAdi + "/" + klasorAdi);
        int sayac = 2;
        while (klasor.exists()) {
            klasorAdi = getDepoAdi(depoUrl) + "_klon_" + sayac;
            klasor = new File(System.getProperty("user.dir"), anaKlasorAdi + "/" + klasorAdi);
            sayac++;
        }
        return klasorAdi;
    }

    // Depo URL'sinden depo adını alma metodu
    private static String getDepoAdi(String depoUrl) {
        String[] parcalar = depoUrl.split("/");
        String depoAdi = parcalar[parcalar.length - 1];
        if (depoAdi.endsWith(".git")) {
            depoAdi = depoAdi.substring(0, depoAdi.length() - 4);
        }
        return depoAdi;
    }

    // Klasörü silme metodu
    private static void klasoruSil(File klasor) {
        File[] dosyalar = klasor.listFiles();
        if (dosyalar != null) {
            for (File dosya : dosyalar) {
                if (dosya.isDirectory()) {
                    klasoruSil(dosya);
                } else {
                    dosya.delete();
                }
            }
        }
        klasor.delete();
    }

    // .java dosyalarını bulma ve sadece sınıf içeren dosyaları bulma metodu
    private static List<File> sadeceSinifDosyalariBul(File klasor) {
        List<File> sinifDosyalari = new ArrayList<>();
        sinifDosyalariniRekursifBul(klasor, sinifDosyalari);
        return sinifDosyalari;
    }

    // Rekürsif olarak .java dosyalarını ve sadece sınıf içeren dosyaları bulma metodu
    private static void sinifDosyalariniRekursifBul(File klasor, List<File> sinifDosyalari) {
        File[] dosyalar = klasor.listFiles();
        if (dosyalar != null) {
          for (File dosya : dosyalar) {
            if (dosya.isDirectory()) {
              sinifDosyalariniRekursifBul(dosya, sinifDosyalari);
            } else if (dosya.getName().endsWith(".java") && icerikSadeceSinifIcerir(dosya) && !icerikArayuzIcerir(dosya)) {
              sinifDosyalari.add(dosya);
            }
          }
        }
      }
      
      private static boolean icerikArayuzIcerir(File dosya) {
        try {
          String icerik = new String(Files.readAllBytes(Paths.get(dosya.getAbsolutePath())));
          return icerik.contains(" interface ");
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }
      }
      
    // Dosyanın içeriğinin sadece sınıf içerip içermediğini kontrol etme metodu
    private static boolean icerikSadeceSinifIcerir(File dosya) {
        try {
            String icerik = new String(Files.readAllBytes(Paths.get(dosya.getAbsolutePath())));
            // İçerikte sınıf tanımını kontrol et (Ör. "class" veya "interface" anahtar kelimelerini ara)
            return icerik.contains(" class ") || icerik.contains(" interface ");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Bosluk satır sayısını hesapla
    private static int boslukSatirSayisi(String icerik) {
        int boslukSatirSayisi = 0;
        String[] satirlar = icerik.split("\\r?\\n");
        for (String satir : satirlar) {
            if (satir.trim().isEmpty()) {
                boslukSatirSayisi++;
            }
        }
        return boslukSatirSayisi;
    }
    
    // Kod satırı sayısını hesapla (yorum ve boşluk satırları hariç)
    private static int kodSatirSayisi(String icerik) {
        String[] satirlar = icerik.split("\\r?\\n");
        int kodSatirSayisi = 0;
        for (String satir : satirlar) {
            // Satırın sadece boşluklardan oluşmadığını ve yorum satırı veya bloğu olmadığını kontrol et
            if (!satir.trim().isEmpty() && !satir.trim().startsWith("//") && !satir.trim().startsWith("/*") && !satir.trim().startsWith("/**") && !satir.trim().startsWith("*")) {
                kodSatirSayisi++;
            }
        }
        return kodSatirSayisi;
    }

    // Javadoc satır sayısını hesapla
    private static int javadocSatirSayisiHesapla(String icerik) {
        int javadocSatirSayisi = 0;
        boolean javadocIcerisinde = false;
      
        String[] satirlar = icerik.split("\\r?\\n");
        for (String satir : satirlar) {
          satir = satir.trim();
      
          if (satir.startsWith("/**")) {
            javadocIcerisinde = true;
          } else if (satir.endsWith("*/")) {
            javadocIcerisinde = false;
          } else if (javadocIcerisinde) {
            javadocSatirSayisi++;
          }
        }
      
        return javadocSatirSayisi;
      }
                
      // Yorum satır sayısını hesapla 
      private static int yorumSatirSayisiHesapla(String icerik) {
        // Regex deseni, yorum satırlarını tanımlamak için kullanılır.
        String regexYorumSatir = "//.*|/\\*(?:[^*]|\\*(?!/))*\\*/";
        Pattern patternYorumSatir = Pattern.compile(regexYorumSatir);
      
        // Regex ile yorum satırlarını arayın ve sayın.
        Matcher matcherYorumSatir = patternYorumSatir.matcher(icerik);
        int yorumSatirSayisi = 0;
        while (matcherYorumSatir.find()) {
            String yorumSatiri = matcherYorumSatir.group();
            // Eğer yorum satırı Javadoc tarzındaysa (/** ... */) ve içindeki * karakterlerini içermiyorsa sayma.
            if (!yorumSatiri.startsWith("/**")) {
                yorumSatirSayisi++;
            }
        }
      
        return yorumSatirSayisi;
    }
        
    // Java dosyasını inceleme metodu
    private static void javaDosyasiniIncele(File dosya) {
        try {
            // Dosyayı oku
            String icerik = new String(Files.readAllBytes(Paths.get(dosya.getAbsolutePath())));

            // Javadoc olarak yorum satır sayısı
            int javadocSatirSayisi = javadocSatirSayisiHesapla(icerik);

            // Diğer yorumlar satır sayısı
            int yorumSatirSayisi = yorumSatirSayisiHesapla(icerik);

            // Kod satır sayısı (tüm yorum ve boşluk satırları hariç)
            int kodSatirSayisi = kodSatirSayisi(icerik);

            // LOC (Line of Code) (Bir dosyadaki her şey dahil satır sayısı)
            int loc = icerik.split("\\n").length;

            // Fonksiyon Sayısı (Sınıfın içinde bulunan tüm fonksiyonların toplam sayısı)
            int fonksiyonSayisi = eslesmeleriGetir(icerik, "\\b(public|private|protected|static|abstract|final|synchronized|native|strictfp)?\\s+(\\w+)(?:\\.<\\w+>)?\\s*\\((.*?)\\)\\s*\\{").size();
            // Yorum Sapma Yüzdesi (Yazılması gereken yorum satır sayısı yüzdelik olarak ne kadar sapmış)
            double YG = ((javadocSatirSayisi + yorumSatirSayisi) * 0.8) / fonksiyonSayisi;
            double YH = ((double) kodSatirSayisi / fonksiyonSayisi) * 0.3;
            double yorumSapmaYuzdesi = ((100 * YG) / YH) - 100;

            // Analiz sonuçlarını yazdırma
            System.out.println("Sınıf: " + dosya.getName());
            System.out.println("Javadoc Satır Sayısı: " + javadocSatirSayisi);
            System.out.println("Yorum Satır Sayısı: " + yorumSatirSayisi);
            System.out.println("Kod Satır Sayısı: " + kodSatirSayisi);
            System.out.println("LOC: " + loc);
            System.out.println("Fonksiyon Sayısı: " + fonksiyonSayisi);
            System.out.printf("Yorum Sapma Yüzdesi: %% %.2f\n", yorumSapmaYuzdesi); // Yüzdenin .'dan sonraki 2 bbasamağını gösteriyor
            System.out.println("-----------------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metni belirli bir desene göre eşleştirme sayısını hesapla
    private static int eslesmeSayisiniHesapla(String metin, String desen) {
        return eslesmeleriGetir(metin, desen).size();
    }

    // Metni belirli bir desene göre eşleştirmeleri döndürme
    private static List<String> eslesmeleriGetir(String metin, String desen) {
        List<String> eslesmeler = new ArrayList<>();
        Matcher eslestirici = Pattern.compile(desen).matcher(metin);
        while (eslestirici.find()) {
            eslesmeler.add(eslestirici.group());
        }
        return eslesmeler;
    }
}
