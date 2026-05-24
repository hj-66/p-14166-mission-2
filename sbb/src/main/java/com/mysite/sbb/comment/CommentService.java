package com.mysite.sbb.comment;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.question.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    public Comment create(String content, Question question) {
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setQuestion(question);

        this.commentRepository.save(comment);
        return comment;
    }

    public Comment modify(Comment comment, String content) {
        comment.setContent(content);
        return comment;
    }

    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }

    public List<Comment> getCommentList(Question question) {
        return commentRepository.findByQuestionId(question.getId());
    }
}
