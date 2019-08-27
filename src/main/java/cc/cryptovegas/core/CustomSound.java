package cc.cryptovegas.core;

import cc.cryptovegas.core.player.CryptoPlayer;
import org.bukkit.Sound;

public enum CustomSound {
    DING(Sound.ORB_PICKUP, 1F, 0F),
    DRINK(Sound.DRINK, 0.5F, 0.5F),
    COUNTDOWN(Sound.NOTE_PLING, 1F, 0F),
    END_COUNTDOWN(Sound.NOTE_PLING, 1F, 1F);

    private final Sound sound;
    private final float volume;
    private final float pitch;

    CustomSound(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void playFor(CryptoPlayer cryptoPlayer) {
        cryptoPlayer.playSound(sound, volume, pitch);
    }
}
