import de.heikoseeberger.sbtheader.{ CommentStyle, CommentBlockCreator }
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport.HeaderPattern.commentBetween

object BuildSettings {
  lazy val customCommentStyle: CommentStyle =
    CommentStyle(
      new CommentBlockCreator("/**", " *", " */"),
      commentBetween("""/\**+""", "*", """\*/""")
    )
}
