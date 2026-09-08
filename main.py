"""Android için TCP/IP Hızlı Gönderim (Kivy)."""
import json
import socket
from pathlib import Path
from threading import Thread

from kivy.app import App
from kivy.clock import Clock
from kivy.lang import Builder
from kivy.properties import ListProperty
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.popup import Popup
from kivy.uix.screenmanager import Screen, ScreenManager
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput

KV = r'''
<RoundedButton>:
    background_normal: ''
    background_down: ''
    background_color: self.background_rgba
    color: 1, 1, 1, 1
    bold: True
    font_size: '16sp'
    canvas.before:
        Color:
            rgba: self.background_rgba
        RoundedRectangle:
            pos: self.pos
            size: self.size
            radius: [14,]
<DarkInput>:
    background_normal: ''
    background_active: ''
    background_color: .07, .08, .11, 1
    foreground_color: 1, 1, 1, 1
    hint_text_color: .6, .64, .73, 1
    cursor_color: .38, .53, 1, 1
    font_size: '16sp'
    padding: '12dp', '12dp'
<Root>:
    canvas.before:
        Color:
            rgba: .075, .082, .12, 1
        Rectangle:
            pos: self.pos
            size: self.size
'''
Builder.load_string(KV)


class RoundedButton(Button):
    background_rgba = ListProperty([0.19, 0.23, 0.34, 1])


class DarkInput(TextInput):
    pass


class Root(ScreenManager):
    pass


class MobilTcpApp(App):
    title = "TCP/IP Hızlı Gönderim"

    def build(self):
        self.kayit_yolu = Path(self.user_data_dir) / "hizli_butonlar.json"
        self.hizli_butonlar = self.kayitlari_oku()
        self.manuel_soket = None
        self.root = Root()
        self.manuel_ekrani_olustur()
        self.hizli_ekrani_olustur()
        return self.root

    def baslik(self, metin, boyut=25):
        return Label(text=metin, size_hint_y=None, height="48dp", font_size=f"{boyut}sp", bold=True, color=(1, 1, 1, 1), halign="left", valign="middle")

    def bilgi(self, metin):
        return Label(text=metin, size_hint_y=None, height="42dp", font_size="14sp", color=(.68, .72, .82, 1), halign="left", valign="middle")

    def alan(self, ipucu, metin="", cok_satir=False, yukseklik="52dp"):
        return DarkInput(text=metin, hint_text=ipucu, multiline=cok_satir, size_hint_y=None, height=yukseklik)

    def ekran_kabugu(self):
        ekran = Screen()
        ana = BoxLayout(orientation="vertical", padding="18dp", spacing="12dp")
        ekran.add_widget(ana)
        return ekran, ana

    def ust_menu(self, ana, aktif):
        satir = BoxLayout(size_hint_y=None, height="50dp", spacing="8dp")
        for ad, hedef in (("Manuel", "manuel"), ("Hızlı Butonlar", "hizli")):
            renk = [.31, .45, 1, 1] if aktif == hedef else [.14, .17, .25, 1]
            dugme = RoundedButton(text=ad, background_rgba=renk)
            dugme.bind(on_release=lambda _, sayfa=hedef: setattr(self.root, "current", sayfa))
            satir.add_widget(dugme)
        ana.add_widget(satir)

    def manuel_ekrani_olustur(self):
        ekran, ana = self.ekran_kabugu(); ekran.name = "manuel"
        ana.add_widget(self.baslik("TCP/IP Hızlı Gönderim")); ana.add_widget(self.bilgi("Manuel bağlantı ile mesaj gönderin.")); self.ust_menu(ana, "manuel")
        self.manuel_ip = self.alan("IP adresi", "127.0.0.1")
        self.manuel_port = self.alan("Port numarası", "5000")
        ana.add_widget(self.manuel_ip); ana.add_widget(self.manuel_port)
        baglanti = BoxLayout(size_hint_y=None, height="54dp", spacing="10dp")
        ac = RoundedButton(text="TCP Aç", background_rgba=[.31, .45, 1, 1]); ac.bind(on_release=lambda _: self.manuel_baglanti_ac())
        kapat = RoundedButton(text="TCP Kapat", background_rgba=[.7, .24, .33, 1]); kapat.bind(on_release=lambda _: self.manuel_baglanti_kapat())
        baglanti.add_widget(ac); baglanti.add_widget(kapat); ana.add_widget(baglanti)
        self.manuel_mesaj = self.alan("Gönderilecek TCP mesajı", cok_satir=True, yukseklik="150dp"); ana.add_widget(self.manuel_mesaj)
        gonder = RoundedButton(text="Mesajı Gönder", size_hint_y=None, height="58dp", background_rgba=[.31, .45, 1, 1]); gonder.bind(on_release=lambda _: self.manuel_mesaj_gonder()); ana.add_widget(gonder)
        self.manuel_durum = self.bilgi("Hazır"); ana.add_widget(self.manuel_durum); ana.add_widget(Label())
        self.root.add_widget(ekran)

    def hizli_ekrani_olustur(self):
        ekran, ana = self.ekran_kabugu(); ekran.name = "hizli"
        ana.add_widget(self.baslik("Hızlı Butonlar")); ana.add_widget(self.bilgi("Tek tıklamayla kayıtlı hedeflere sabit kod gönderin.")); self.ust_menu(ana, "hizli")
        yeni = RoundedButton(text="+ Yeni Fonksiyon Butonu", size_hint_y=None, height="56dp", background_rgba=[.31, .45, 1, 1]); yeni.bind(on_release=lambda _: self.duzenleme_penceresi())
        ana.add_widget(yeni)
        kaydir = ScrollView(); self.hizli_liste = BoxLayout(orientation="vertical", size_hint_y=None, spacing="10dp", padding=(0, "4dp")); self.hizli_liste.bind(minimum_height=self.hizli_liste.setter("height")); kaydir.add_widget(self.hizli_liste); ana.add_widget(kaydir)
        self.root.add_widget(ekran); self.hizli_listeyi_yenile()

    def hizli_listeyi_yenile(self):
        if not hasattr(self, "hizli_liste"): return
        self.hizli_liste.clear_widgets()
        if not self.hizli_butonlar:
            self.hizli_liste.add_widget(self.bilgi("Henüz hızlı buton oluşturulmadı.")); return
        for index, kayit in enumerate(self.hizli_butonlar):
            kart = BoxLayout(orientation="vertical", size_hint_y=None, height="145dp", padding="12dp", spacing="7dp")
            kart.add_widget(Label(text=kayit["ad"], font_size="18sp", bold=True, color=(1, 1, 1, 1), halign="left", valign="middle", size_hint_y=None, height="30dp"))
            ips = self.kayit_ips(kayit)
            kart.add_widget(Label(text=f"{len(ips)} hedef  •  Port {kayit['port']}  •  Kod: {kayit['mesaj']}", color=(.68, .72, .82, 1), font_size="13sp", halign="left", valign="middle", size_hint_y=None, height="30dp"))
            satir = BoxLayout(size_hint_y=None, height="45dp", spacing="7dp")
            gonder = RoundedButton(text="Gönder", background_rgba=[.31, .45, 1, 1]); gonder.bind(on_release=lambda _, veri=kayit: self.hizli_gonder(veri))
            duzenle = RoundedButton(text="Düzenle"); duzenle.bind(on_release=lambda _, sira=index: self.duzenleme_penceresi(sira))
            sil = RoundedButton(text="Sil", background_rgba=[.7, .24, .33, 1]); sil.bind(on_release=lambda _, sira=index: self.kaydi_sil(sira))
            satir.add_widget(gonder); satir.add_widget(duzenle); satir.add_widget(sil); kart.add_widget(satir); self.hizli_liste.add_widget(kart)

    def duzenleme_penceresi(self, index=None):
        kayit = self.hizli_butonlar[index] if index is not None else {"ad": "", "ips": ["127.0.0.1"], "port": 5000, "mesaj": ""}
        govde = BoxLayout(orientation="vertical", padding="18dp", spacing="10dp")
        govde.add_widget(self.baslik("Butonu Düzenle" if index is not None else "Yeni Fonksiyon Butonu", 19))
        ad = self.alan("Buton adı", kayit["ad"]); ips = self.alan("IP'ler: satır satır veya virgülle", "\n".join(self.kayit_ips(kayit)), True, "85dp")
        port = self.alan("Port", str(kayit["port"])); mesaj = self.alan("Sabit TCP kodu", kayit["mesaj"])
        for oge in (ad, ips, port, mesaj): govde.add_widget(oge)
        eylemler = BoxLayout(size_hint_y=None, height="55dp", spacing="8dp")
        popup = Popup(title="", content=govde, size_hint=(.92, .78), auto_dismiss=False, separator_height=0, background="", background_color=(.12, .14, .2, 1))
        kaydet = RoundedButton(text="Kaydet", background_rgba=[.31, .45, 1, 1]); vazgec = RoundedButton(text="Vazgeç")
        kaydet.bind(on_release=lambda _: self.kaydi_kaydet(index, ad.text, ips.text, port.text, mesaj.text, popup)); vazgec.bind(on_release=lambda _: popup.dismiss())
        eylemler.add_widget(kaydet); eylemler.add_widget(vazgec); govde.add_widget(eylemler); popup.open()

    def kaydi_kaydet(self, index, ad, ip_metni, port, mesaj, popup):
        ips = self.ipleri_ayir(ip_metni)
        try: port = int(port)
        except ValueError: port = 0
        if not ad.strip() or not ips or not mesaj or not 1 <= port <= 65535:
            self.uyari("Buton adı, en az bir IP, 1-65535 arası port ve kod gereklidir."); return
        veri = {"ad": ad.strip(), "ips": ips, "port": port, "mesaj": mesaj}
        if index is None: self.hizli_butonlar.append(veri)
        else: self.hizli_butonlar[index] = veri
        self.kayitlari_kaydet(); self.hizli_listeyi_yenile(); popup.dismiss()

    def kaydi_sil(self, index):
        self.hizli_butonlar.pop(index); self.kayitlari_kaydet(); self.hizli_listeyi_yenile()

    def manuel_baglanti_ac(self):
        self.manuel_durum.text = "Bağlantı kuruluyor..."
        def islem():
            try:
                self.manuel_soket = socket.create_connection((self.manuel_ip.text.strip(), int(self.manuel_port.text)), timeout=5)
                Clock.schedule_once(lambda _: self.durum_yaz("Manuel TCP bağlantısı açık."))
            except (OSError, ValueError) as hata: Clock.schedule_once(lambda _: self.durum_yaz(f"Bağlantı hatası: {hata}"))
        Thread(target=islem, daemon=True).start()

    def manuel_baglanti_kapat(self):
        if self.manuel_soket: self.manuel_soket.close(); self.manuel_soket = None
        self.durum_yaz("Manuel TCP bağlantısı kapalı.")

    def manuel_mesaj_gonder(self):
        if not self.manuel_soket: self.uyari("Önce TCP bağlantısını açın."); return
        metin = self.manuel_mesaj.text
        if not metin: self.uyari("Gönderilecek bir mesaj girin."); return
        Thread(target=lambda: self._manuel_yolla(metin), daemon=True).start()

    def _manuel_yolla(self, metin):
        try: self.manuel_soket.sendall(metin.encode("utf-8")); Clock.schedule_once(lambda _: self.durum_yaz("Mesaj gönderildi."))
        except OSError as hata: Clock.schedule_once(lambda _: self.durum_yaz(f"Gönderim hatası: {hata}"))

    def hizli_gonder(self, kayit):
        def islem():
            basarili, hatali = 0, []
            for ip in self.kayit_ips(kayit):
                try:
                    with socket.create_connection((ip, kayit["port"]), timeout=5) as soket: soket.sendall(kayit["mesaj"].encode("utf-8"))
                    basarili += 1
                except OSError as hata: hatali.append(f"{ip}: {hata}")
            Clock.schedule_once(lambda _: self.uyari(f"{basarili} hedefe gönderildi." + ("\n\nHatalar:\n" + "\n".join(hatali) if hatali else "")))
        Thread(target=islem, daemon=True).start()

    def durum_yaz(self, metin): self.manuel_durum.text = metin
    def uyari(self, metin): Popup(title="Bilgi", content=Label(text=metin), size_hint=(.8, .3)).open()
    def ipleri_ayir(self, metin): return [ip.strip() for ip in metin.replace(",", "\n").splitlines() if ip.strip()]
    def kayit_ips(self, kayit): return kayit.get("ips") or ([kayit["ip"]] if kayit.get("ip") else [])
    def kayitlari_oku(self):
        try: return json.loads(self.kayit_yolu.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError): return []
    def kayitlari_kaydet(self):
        self.kayit_yolu.parent.mkdir(parents=True, exist_ok=True); self.kayit_yolu.write_text(json.dumps(self.hizli_butonlar, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    MobilTcpApp().run()
