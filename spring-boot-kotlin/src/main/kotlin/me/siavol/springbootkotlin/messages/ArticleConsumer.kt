package me.siavol.springbootkotlin.messages

import lombok.extern.slf4j.Slf4j
import me.siavol.springbootkotlin.Article
import me.siavol.springbootkotlin.ArticleRepository
import me.siavol.springbootkotlin.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@Slf4j
class ArticleConsumer(private val userRepository: UserRepository,
                      private val articleRepository: ArticleRepository) {
    var logger: org.slf4j.Logger = LoggerFactory.getLogger(ArticleConsumer::class.java)

    @KafkaListener(topics = ["created-article"])
    fun processCreatedMessage(newArticle: Article) {
        logger.info("Received new article '${newArticle.title}'")
        userRepository.save(newArticle.author)
        articleRepository.save(newArticle)
    }
}