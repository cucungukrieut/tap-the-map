package pl.gov.stat.tapthemap.game_ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.gov.stat.tapthemap.BuildConfig;
import pl.gov.stat.tapthemap.Country;
import pl.gov.stat.tapthemap.NoConnectionActivity;
import pl.gov.stat.tapthemap.R;
import pl.gov.stat.tapthemap.ar.ViewMatrixOverrideCamera;
import pl.gov.stat.tapthemap.gameplay.Answer;
import pl.gov.stat.tapthemap.gameplay.Gameplay;
import pl.gov.stat.tapthemap.gameplay.Question;
import pl.gov.stat.tapthemap.scene.CountryInstance;
import pl.gov.stat.tapthemap.scene.Map;
import pl.gov.stat.tapthemap.scene.MapRenderer;

/**
 * Class responsible for displaying the question series fragment.
 */
public class QuestionSeriesFragment extends Fragment implements View.OnTouchListener {

    private InteractionListener mListener;

    @BindView(R.id.button_close)
    View mCloseView;
    @BindView(R.id.question)
    TextView mQuestionTextView;
    @BindView(R.id.round_progress)
    TextView mRoundProgress;
    @BindView(R.id.button_zoom_in)
    View mZoomIn;
    @BindView(R.id.button_zoom_out)
    View mZoomOut;

    @BindView(R.id.legend_high_text)
    TextView mHighTextView;
    @BindView(R.id.legend_mid_text)
    TextView mMidTextView;
    @BindView(R.id.legend_low_text)
    TextView mLowTextView;
    @BindView(R.id.legend_high_color)
    View mHighColorView;
    @BindView(R.id.legend_mid_color)
    View mMidColorView;
    @BindView(R.id.legend_low_color)
    View mLowColorView;
    @BindView(R.id.next_button_icon)
    ImageView mNextIcon;

    @BindView(R.id.answers_recycler)
    RecyclerView mAnswersRecycler;
    @BindView(R.id.answers_container)
    LinearLayout mAnswersContainer;

    @BindView(R.id.remaining_container)
    LinearLayout mRemainingContainer;

    private Gameplay gameplay;
    boolean showingAnswers = false;
    private AnswersAdapter answersAdapter;

    private ViewMatrixOverrideCamera camera;
    private Map map;
    private MapRenderer renderer;
    private List<Question> questionList;

    public QuestionSeriesFragment() {
        // Required empty public constructor
    }

    /**
     * Initialize.
     */
    public void init(MapRenderer renderer, ViewMatrixOverrideCamera camera, List<Question> questionList) {
        this.renderer = renderer;
        this.map = renderer.getMap();
        this.camera = camera;
        this.questionList = questionList;
    }

    /**
     * When create view.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_question_series, container, false);
        ButterKnife.bind(this, view);

        setupAnswersRecycler();
        startGame();

        return view;
    }

    /**
     * Setup answers recycler.
     */
    public void setupAnswersRecycler() {
        mAnswersRecycler.setHasFixedSize(true);
        mAnswersRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        answersAdapter = new AnswersAdapter(getResources(), renderer);
        mAnswersRecycler.setAdapter(answersAdapter);
    }

    /**
     * Start game.
     */
    public void startGame() {
        showingAnswers = false;
        map.setVisible(true);
        mAnswersContainer.setVisibility(View.INVISIBLE);
        mRemainingContainer.setVisibility(View.VISIBLE);

        gameplay = new Gameplay(questionList, Gameplay.Settings.COUNTRIES_PER_QUESTION);
        try {
            Question question = gameplay.getCurrentQuestion();
            showQuestion(question);
        } catch (Exception e) {
            e.printStackTrace();
            getActivity().startActivity(new Intent(getActivity(), NoConnectionActivity.class));
            getActivity().finish();
        }
    }

    /**
     * Show question.
     *
     * @param question Question to show
     */
    private void showQuestion(Question question) {
        showingAnswers = false;
        map.reset();
        question.getCountries().forEach(map::enableCountry);

        initLabels();
        mRemainingContainer.getChildAt(0).setVisibility(View.VISIBLE);

        mQuestionTextView.setText(question.getDesc());
        mRoundProgress.setText(getString(R.string.question_progress_counter, gameplay.getCurrentQuestionInt() + 1, Gameplay.Settings.QUESTIONS_PER_SERIES));

        mHighTextView.setText(question.getMaxLabel());
        mMidTextView.setText(question.getMidLabel());
        mLowTextView.setText(question.getMinLabel());

        int maxColor = question.getCategory().getMaxColor();
        int minColor = question.getCategory().getMinColor();
        int midColor = ColorUtils.blendARGB(minColor, maxColor, 0.5f);

        map.setColors(minColor, maxColor);

        mHighColorView.setBackgroundColor(maxColor);
        mMidColorView.setBackgroundColor(midColor);
        mLowColorView.setBackgroundColor(minColor);
    }

    public void initLabels() {
        for (int i=1; i < mRemainingContainer.getChildCount(); i++) {
            ImageView plate = (ImageView) mRemainingContainer.getChildAt(i);
            plate.setTag(null);
            plate.setOnClickListener(new CenterOnCountryFromTagTouchListener(renderer));
        }

        map.getEnabledCountries().forEach(((country, countryInstance) -> {
            for (int i=1; i < mRemainingContainer.getChildCount(); i++) {
                ImageView plate = (ImageView) mRemainingContainer.getChildAt(i);
                if (plate.getTag() != null) {
                    continue;
                }
                int plateResId = getResources().getIdentifier("country_" + country.getEuCode() + "_plate", "drawable", BuildConfig.APPLICATION_ID);
                plate.setImageResource(plateResId);
                plate.setTag(country);
                plate.setVisibility(View.VISIBLE);
                break;
            }
        }));
    }

    public void updateLabels() {
        Supplier<Stream<java.util.Map.Entry<Country, CountryInstance>>> enabled = () -> map.getEnabledCountries().entrySet().stream();
        enabled.get().forEach(e -> displayLabel(e.getKey(), e.getValue()));

        long remainingCount = enabled.get().filter(e -> e.getValue().getHeight() == 0).count();
        mRemainingContainer.getChildAt(0).setVisibility(remainingCount > 0 ? View.VISIBLE : View.GONE);
    }


    private void displayLabel(Country country, CountryInstance instance) {
        for (int i=1; i < mRemainingContainer.getChildCount(); i++) {
            ImageView plate = (ImageView) mRemainingContainer.getChildAt(i);
            if (plate.getTag().equals(country)) {
                plate.setVisibility(instance.getHeight() == 0 ? View.VISIBLE : View.GONE);
            }
        }
    }

    /**
     * Button close.
     */
    @OnClick(R.id.button_close)
    public void buttonClose() {
        mListener.onCloseTriggered();
    }

    /**
     * Zoomed in.
     */
    @OnClick(R.id.button_zoom_in)
    public void zoomIn() {
        camera.zoomIn();
    }

    @OnClick(R.id.button_reset)
    public void resetMap() {
        camera.setZoom(1.0f);
        renderer.setWorldOffset(renderer.getDefaultWorldOffset());
    }

    /**
     * Zoomed out.
     */
    @OnClick(R.id.button_zoom_out)
    public void zoomOut() {
        camera.zoomOut();
    }

    /**
     * When next button clicked.
     */
    @OnClick(R.id.next_button)
    public void onNextButtonClick(View view) {
        if (showingAnswers) {
            mNextIcon.setImageResource(R.drawable.ic_done_24dp);
            showingAnswers = false;
            mAnswersContainer.setVisibility(View.INVISIBLE);
            mRemainingContainer.setVisibility(View.VISIBLE);

            Question nextQuestion = gameplay.finishQuestion(getActivity());
            if (nextQuestion == null) {
                mListener.onSeriesDone(gameplay.getResult(), gameplay.getMaxResult());
                return;
            }
            showQuestion(nextQuestion);
        } else {
            gameplay.updateScore(map);
            Question currentQuestion = gameplay.getCurrentQuestion();
            List<Country> countries = currentQuestion.getCountries();
            List<Answer> answers = currentQuestion.getAnswers();
            answers.sort(Comparator.comparing(Answer::getValueRaw).reversed());
            answersAdapter.updateAnswers(answers);

            mRemainingContainer.setVisibility(View.INVISIBLE);
            mAnswersContainer.setVisibility(View.VISIBLE);
            mNextIcon.setImageResource(R.drawable.ic_trending_flat_24dp);
            showingAnswers = true;
            for (Country country : countries) {
                map.enableCountry(country);
                CountryInstance countryInstance = map.getCountry(country);
                if (countryInstance != null) {
                    int targetHeight = currentQuestion.getCorrectAnswer(country);
                    countryInstance.setHeight(targetHeight);
                }
            }
        }
    }

    /**
     * When touched.
     */
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View view, MotionEvent event) {
        if (!showingAnswers) {
            renderer.onTouchEvent(event);
        }
        return false;
    }

    /**
     * When attached.
     *
     * @param context Context of the application
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof InteractionListener) {
            mListener = (InteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement InteractionListener");
        }
    }

    /**
     * When detached.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Interaction listener.
     */
    public interface InteractionListener {
        /**
         * When close triggered.
         */
        void onCloseTriggered();

        /**
         * When series done.
         *
         * @param score Result
         * @param outOf Maximum result
         */
        void onSeriesDone(int score, int outOf);
    }
}
