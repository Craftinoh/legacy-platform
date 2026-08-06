package fr.xephi.authme.message.locale;

import org.bukkit.entity.Player;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link ClientLocaleProvider}.
 */
public class ClientLocaleProviderTest {

    private final ClientLocaleProvider clientLocaleProvider = new ClientLocaleProvider();

    // Normalization itself is covered by SupportedLanguagesTest, which is where it now lives.

    @Test
    public void shouldReturnNullForNullPlayer() {
        assertThat(clientLocaleProvider.getLanguageCode(null), nullValue());
    }

    @Test
    public void shouldReadLocaleThroughPlayerGetLocale() {
        // given - a Bukkit API that has Player#getLocale(), i.e. Minecraft 1.12 and later
        Player player = mock(Player.class);
        org.mockito.BDDMockito.given(player.getLocale()).willReturn("it_IT");

        // when / then
        assertThat(clientLocaleProvider.getLanguageCode(player), equalTo("it"));
    }

    @Test
    public void shouldNotFailWhenLocaleCannotBeRead() {
        // given - a player whose locale accessor blows up
        Player player = mock(Player.class);
        org.mockito.BDDMockito.given(player.getLocale()).willThrow(new UnsupportedOperationException());

        // when / then - message sending must never break because of locale detection
        assertThat(clientLocaleProvider.getLanguageCode(player), nullValue());
    }

    @Test
    public void shouldStayUnsupportedAfterAFailureUntilReloaded() {
        // given
        Player player = mock(Player.class);
        org.mockito.BDDMockito.given(player.getLocale()).willThrow(new UnsupportedOperationException());
        assertThat(clientLocaleProvider.getLanguageCode(player), nullValue());

        // when
        clientLocaleProvider.reload();
        Player workingPlayer = mock(Player.class);
        org.mockito.BDDMockito.given(workingPlayer.getLocale()).willReturn("en_GB");

        // then - the strategy is re-evaluated after a reload
        assertThat(clientLocaleProvider.getLanguageCode(workingPlayer), equalTo("en"));
    }
}
