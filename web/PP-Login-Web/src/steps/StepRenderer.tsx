import EnterIdentifier from "@/steps/EnterIdentifier";
import EnterPassword from "@/steps/EnterPassword";
import VerifyCode from "@/steps/VerifyCode";
import ChooseMfa from "@/steps/ChooseMfa";
import Done from "@/steps/Done";

export default function StepRenderer(props: {
    step: string;
    data: Record<string, any>;
    onSubmit: (action: string, input?: Record<string, any>) => void;
}) {
    const { step, data, onSubmit } = props;

    switch (step) {
        // 通用：输入账号（邮箱/手机号/用户名）
        case "ENTER_IDENTIFIER":
            return <EnterIdentifier onSubmit={onSubmit} />;

        // 登录：输入密码
        case "ENTER_PASSWORD":
            return <EnterPassword onSubmit={onSubmit} />;

        // 短信/邮箱验证码：校验
        case "VERIFY_CODE":
            return <VerifyCode data={data} onSubmit={onSubmit} />;

        // MFA：选择方式（TOTP / WebAuthn / SMS…）
        case "CHOOSE_MFA":
            return <ChooseMfa data={data} onSubmit={onSubmit} />;

        case "DONE":
            return <Done data={data} />;

        default:
            return (
                <div>
                    <div style={{ marginBottom: 8 }}>Unknown step: <b>{step}</b></div>
                    <pre style={{ background: "#f6f6f6", padding: 12 }}>
                        {JSON.stringify({ step, data }, null, 2)}
                    </pre>
                </div>
            );
    }
}